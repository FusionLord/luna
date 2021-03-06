package io.luna.game.plugin;

import com.google.common.io.Files;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.luna.LunaContext;
import io.luna.game.GameService;
import io.luna.game.event.EventListenerPipelineSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;


import static org.apache.logging.log4j.util.Unbox.box;

/**
 * A bootstrapper that initializes and evaluates all {@code Scala} dependencies and plugins.
 *
 * @author lare96 <http://github.org/lare96>
 */
public final class PluginBootstrap implements Callable<EventListenerPipelineSet> {

    /**
     * A callback that will swap old event pipelines out for newly constructed ones.
     */
    private final class PluginBootstrapCallback implements FutureCallback<EventListenerPipelineSet> {

        @Override
        public void onSuccess(EventListenerPipelineSet result) {
            PluginManager plugins = context.getPlugins();
            GameService service = context.getService();

            service.sync(() -> plugins.getPipelines().swap(result));
        }

        @Override
        public void onFailure(Throwable t) {
            LOGGER.catching(t);
        }
    }

    /**
     * The asynchronous logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The directory containing plugin files.
     */
    private static final String DIR = "./plugins/";

    /**
     * The pipeline set.
     */
    private final EventListenerPipelineSet pipelines = new EventListenerPipelineSet();

    /**
     * A map of file names to their contents.
     */
    private final Map<String, String> files = new HashMap<>();

    /**
     * The current file being evaluated.
     */
    private final AtomicReference<String> currentFile = new AtomicReference<>();

    /**
     * The context instance.
     */
    private final LunaContext context;

    /**
     * The script engine evaluating {@code Scala} code.
     */
    private final ScriptEngine engine;

    /**
     * Creates a new {@link PluginBootstrap}.
     *
     * @param context The context instance.
     */
    public PluginBootstrap(LunaContext context) {
        this.context = context;
        engine = new ScriptEngineManager(ClassLoader.getSystemClassLoader()).getEngineByName("scala");
    }

    @Override
    public EventListenerPipelineSet call() throws Exception {
        init();
        LOGGER.info("A total of {} Scala plugin files were successfully interpreted.", box(files.size()));
        return pipelines;
    }

    /**
     * Initializes this bootstrap using the argued listening executor.
     */
    public void load(ListeningExecutorService service) {
        Executor directExecutor = MoreExecutors.directExecutor();

        Futures.addCallback(service.submit(new PluginBootstrap(context)), new PluginBootstrapCallback(),
                directExecutor);
    }

    /**
     * Initializes this bootstrap using the default listening executor.
     */
    public void load() {
        GameService service = context.getService();
        Executor directExecutor = MoreExecutors.directExecutor();

        Futures.addCallback(service.submit(new PluginBootstrap(context)), new PluginBootstrapCallback(),
                directExecutor);
    }

    /**
     * Initializes this bootstrapper, loading all of the plugins.
     */
    private void init() throws Exception {
        initFiles();
        initDependencies();
        initPlugins();
    }

    /**
     * Parses files in the plugin directory and caches their contents.
     */
    private void initFiles() throws Exception {
        Iterable<File> dirFiles = Files.fileTraverser().depthFirstPreOrder(new File(DIR));

        for (File file : dirFiles) {
            if (file.isFile() && file.getName().endsWith(".scala")) {
                files.put(file.getName(), Files.asCharSource(file, StandardCharsets.UTF_8).read());
            }
        }
    }

    /**
     * Injects state into the script engine and evaluates dependencies.
     */
    private void initDependencies() throws Exception {
        engine.put("$context$", context);
        engine.put("$logger$", LOGGER);
        engine.put("$pipelines$", pipelines);

        currentFile.set("bootstrap.scala");

        String bootstrap = files.remove(currentFile.get());
        splitAndRunModules(bootstrap);
    }

    /**
     * Splits the Scala bootstrap up into modules.
     */
    private void splitAndRunModules(String bootstrap) throws ScriptException {
        // TODO Cleanup, error handling by module. Temporary workaround.
        StringBuilder eval = new StringBuilder();
        boolean first = true;
        try (Scanner sc = new Scanner(bootstrap)) {
            while (sc.hasNextLine()) {
                String nextLine = sc.nextLine();
                if (nextLine.contains("$startModule$(\"module_")) {
                    if (first) {
                        first = false;
                    } else {
                        engine.eval(eval.toString());
                        eval.setLength(0);
                    }
                } else {
                    eval.append(nextLine).append('\n');
                }
            }
            engine.eval(eval.toString());
        }
    }

    /**
     * Evaluates the rest of the normal plugins.
     */
    private void initPlugins() throws Exception {
        for (Entry<String, String> fileEntry : files.entrySet()) {
            currentFile.set(fileEntry.getKey());
            engine.eval(fileEntry.getValue());
        }
    }
}
