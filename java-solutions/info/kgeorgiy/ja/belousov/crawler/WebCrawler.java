package info.kgeorgiy.ja.belousov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Concurrent web crawler implementation, according to Crawler interface
 */
public class WebCrawler implements Crawler {
    final Downloader downloader;

    final ExecutorService downloaderPool;
    final ExecutorService extractorPool;


    /**
     * Basic constructor of web crawler
     *
     * @param downloader  Downloader implementation that will be used to download all links found
     * @param downloaders maximum number of concurrent downloads
     * @param extractors  maximum number of concurrent html parsers for link extraction
     * @param perHost     reserved
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downloaderPool = Executors.newFixedThreadPool(downloaders);
        extractorPool = Executors.newFixedThreadPool(extractors);
    }

    /**
     * Main function used as entrypoint when launched as a standalone application
     *
     * @param args Required:
     *             - URL (String)
     *             Optional:
     *             - Depth (int) - crawling depth
     *             - downloads (int) - max number of concurrent downloads
     *             - extractors (int) - max number of concurrent extractors
     *             - perHost (int) - reserved
     */
    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.err.println("Usage: java WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        try {
            String url = args[0];
            int depth = 5;
            if (args.length >= 2) {
                depth = Integer.parseInt(args[1]);
            }
            int downloads = 3;
            if (args.length >= 3) {
                downloads = Integer.parseInt(args[2]);
            }
            int extractors = Runtime.getRuntime().availableProcessors();
            if (args.length >= 4) {
                extractors = Integer.parseInt(args[3]);
            }
            int perHost = 3;
            if (args.length >= 5) {
                perHost = Integer.parseInt(args[4]);
            }


            try (WebCrawler crawler = new WebCrawler(new CachingDownloader(1.f), downloads, extractors, perHost)) {
                Result result = crawler.download(url, depth);
                for (String i : result.getDownloaded()) {
                    System.out.println(i);
                }
                for (Map.Entry<String, IOException> i : result.getErrors().entrySet()) {
                    System.out.format("%s: Error(%s)%n", i.getKey(), i.getValue());
                }
            } catch (IOException e) {
                System.err.format("Downloader initialization error: %s%n", e.getMessage());
            }
        } catch (NumberFormatException e) {
            System.err.println("Incorrect args format!");
        }
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> downloads = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();

        Phaser countDown = new Phaser(1);

        List<Phaser> synchronizer = new ArrayList<>(depth + 1);
        for (int i = 0; i <= depth; i++) {
            synchronizer.add(new Phaser(0));
        }

        downloadImpl(url, depth, countDown, downloads, errors, synchronizer);

        countDown.arriveAndAwaitAdvance();

        downloads.removeAll(errors.keySet());

        return new Result(new ArrayList<>(downloads), errors);
    }

    @Override
    public void close() {
        downloaderPool.shutdownNow();
        extractorPool.shutdownNow();
    }

    /**
     * Implementation that submits the given download task to the available thread pool.
     *
     * @param url          link to download from
     * @param depth        maximum depth for crawling
     * @param countDown    used to count remaining pages to download, must indicate the number of active downloads before
     *                     the method was called (when this method is called for the first download, it must indicate 0)
     * @param downloads    <em>Thread-safe</em> set of already downloaded links
     * @param errors       <em>Thread-safe</em> map of erroneous URLs
     * @param synchronizer List of exactly {@code depth}+1 {@link Phaser} instances for synchronizing bfs layers.
     */
    private void downloadImpl(String url, int depth, Phaser countDown, Set<String> downloads,
                              ConcurrentMap<String, IOException> errors, List<Phaser> synchronizer) {
        if (downloads.add(url)) {
            countDown.register();
            synchronizer.get(depth - 1).register();
            downloaderPool.submit(() -> {
                try {
                    Document document = downloader.download(url);

                    synchronizer.get(depth).register();
                    synchronizer.get(depth).arriveAndAwaitAdvance();
                    synchronizer.get(depth).arriveAndDeregister();


                    extractorPool.submit(() -> {
                        try {
                            if (depth > 1) {
                                List<String> links = document.extractLinks();
                                for (String link : links) {
                                    downloadImpl(link, depth - 1, countDown, downloads, errors, synchronizer);
                                }
                            }
                        } catch (IOException e) {
                            errors.put(url, e);
                        } finally {
                            countDown.arriveAndDeregister();
                            synchronizer.get(depth - 1).arriveAndDeregister();
                        }
                    });
                } catch (IOException e) {
                    errors.put(url, e);
                    countDown.arriveAndDeregister();
                    synchronizer.get(depth - 1).arriveAndDeregister();
                }
            });
        }
    }
}
