package info.kgeorgiy.ja.shvetsov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;

import java.net.MalformedURLException;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class WebCrawler implements NewCrawler {
    private final Downloader downloader;

    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;

    private final ConcurrentMap<String, Host> hosts;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;

        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;

        this.hosts = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        if (args == null || !(1 <= args.length && args.length <= 5)) {
            System.err.println("Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        String url = args[0];
        int depth = getInteger(args, 1, 1);
        int downloads = getInteger(args, 2, 1);
        int extractors = getInteger(args, 3, 1);
        int perHost = getInteger(args, 4, 2);

        try (WebCrawler crawler = new WebCrawler(new CachingDownloader(1.0), downloads, extractors, perHost)) {
            Result result = crawler.download(url, depth);

            System.out.println("Downloaded " + result.getDownloaded().size() + " pages:");
            result.getDownloaded().forEach(System.out::println);

            System.out.println("Errors occurred in " + result.getErrors().size() + " pages:");
            result.getErrors().forEach((page, error) -> System.out.println(page + ", error: " + error));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns integer in the array. If the index does not exist, return default value.
     *
     * @param args  given array in which integer should be found
     * @param index index of the integer
     * @param def   default value
     * @return parsed integer from the given array
     */
    private static int getInteger(String[] args, int index, int def) {
        if (index > args.length)
            return def;

        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            System.err.println("Not a number - args[" + index + "]: " + args[index] + ". Using default value: " + def);
            return def;
        }
    }

    private List<String> download(List<String> urls, Set<String> downloadedPages,
                                  ConcurrentMap<String, IOException> errorPages,
                                  Set<String> usedLinks, Set<String> excludes,
                                  boolean isLastLayer) {
        Queue<Future<List<String>>> futures = new ConcurrentLinkedQueue<>();
        CountDownLatch latch = new CountDownLatch(urls.size());

        urls.forEach(link -> {
            try {
                if (excludes.stream().noneMatch(link::contains)) {
                    Host host = hosts.computeIfAbsent(URLUtils.getHost(link), (newHost) -> new Host(downloaders, perHost));

                    host.addTask(() -> {
                        try {
                            Document downloadedDocument = downloader.download(link);
                            downloadedPages.add(link);

                            if (!isLastLayer) {
                                futures.add(extractors.submit(() -> {
                                    try {
                                        return downloadedDocument.extractLinks();
                                    } catch (IOException e) {
                                        errorPages.put(link, e);

                                        return List.of();
                                    }
                                }));
                            }
                        } catch (IOException e) {
                            errorPages.put(link, e);
                        }

                        host.releaseSemaphore();
                        host.execute();

                        latch.countDown();
                    });
                } else {
                    latch.countDown();
                }
            } catch (MalformedURLException e) {
                errorPages.put(link, e);
            }
        });

        try {
            latch.await();

            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            return Collections.<String>emptyList();
                        }
                    })
                    .flatMap(List::stream)
                    .distinct()
                    .filter(url -> !usedLinks.contains(url))
                    .peek(usedLinks::add)
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return List.of();
    }

    @Override
    public Result download(String url, int depth, Set<String> excludes) {
        Set<String> downloadedPages = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> errorPages = new ConcurrentHashMap<>();

        Set<String> usedLinks = new HashSet<>();
        usedLinks.add(url);

        List<String> currentLayer = List.of(url);

        for (int i = 0; i < depth; i++) {
            if (currentLayer.isEmpty()) {
                break;
            }

            currentLayer = download(currentLayer, downloadedPages, errorPages, usedLinks, excludes, (i == depth - 1));
        }

        return new Result(new ArrayList<>(downloadedPages), errorPages);
    }

    @Override
    public void close() {
        if (downloaders != null)
            downloaders.close();
        if (extractors != null)
            extractors.close();
    }
}