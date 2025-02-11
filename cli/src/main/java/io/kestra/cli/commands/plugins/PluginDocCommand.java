package io.kestra.cli.commands.plugins;

import com.google.common.base.Charsets;
import io.kestra.cli.AbstractCommand;
import io.kestra.core.docs.DocumentationGenerator;
import io.kestra.core.docs.JsonSchemaGenerator;
import io.kestra.core.plugins.PluginRegistry;
import io.kestra.core.plugins.RegisteredPlugin;
import io.kestra.core.serializers.JacksonMapper;
import io.micronaut.context.ApplicationContext;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@CommandLine.Command(
    name = "doc",
    description = "write documentation for all plugins currently installed"
)
public class PluginDocCommand extends AbstractCommand {
    @Inject
    private ApplicationContext applicationContext;

    @CommandLine.Parameters(index = "0", description = "Path to write documentations files")
    private Path output = Paths.get(System.getProperty("user.dir"), "docs");

    @CommandLine.Option(names = {"--core"}, description = "Also write core tasks docs files")
    private boolean core = false;

    @CommandLine.Option(names = {"--icons"}, description = "Also write icon for each task")
    private boolean icons = false;

    @CommandLine.Option(names = {"--schema"}, description = "Also write json schema for each task")
    private boolean schema = false;

    @Override
    public Integer call() throws Exception {
        super.call();
        DocumentationGenerator documentationGenerator = applicationContext.getBean(DocumentationGenerator.class);

        List<RegisteredPlugin> plugins = core ?  pluginRegistry().plugins() : pluginRegistry().externalPlugins();
        for (RegisteredPlugin registeredPlugin : plugins) {
            documentationGenerator
                .generate(registeredPlugin)
                .forEach(s -> {
                        File file = Paths.get(output.toAbsolutePath().toString(), s.getPath()).toFile();

                        if (!file.getParentFile().exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            file.getParentFile().mkdirs();
                        }

                        try {
                            com.google.common.io.Files
                                .asCharSink(
                                    file,
                                    Charsets.UTF_8
                                ).write(s.getBody());
                            stdOut("Generate doc in: {0}", file);

                            if (s.getIcon() != null && this.icons) {
                                File iconFile = new File(
                                    file.getParent(),
                                    file.getName().substring(0, file.getName().lastIndexOf(".")) + ".svg"
                                );

                                com.google.common.io.Files
                                    .asByteSink(iconFile)
                                    .write(Base64.getDecoder().decode(s.getIcon().getBytes(StandardCharsets.UTF_8)));
                                stdOut("Generate icon in: {0}", iconFile);
                            }

                            if (this.schema && s.getSchema() != null) {
                                File jsonSchemaFile = new File(
                                    file.getParent(),
                                    file.getName().substring(0, file.getName().lastIndexOf(".")) + ".json"
                                );

                                com.google.common.io.Files
                                    .asByteSink(jsonSchemaFile)
                                    .write(JacksonMapper.ofJson().writeValueAsBytes(s.getSchema()));
                                stdOut("Generate json schema in: {0}", jsonSchemaFile);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                );
        }

        return 0;
    }
}
