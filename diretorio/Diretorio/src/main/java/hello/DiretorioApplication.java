package hello;

import hello.resources.EmissaoResource;
import hello.resources.ExchangeResource;
import hello.resources.LeilaoResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import hello.resources.HelloResource;
import hello.health.TemplateHealthCheck;

public class DiretorioApplication extends Application<DiretorioConfiguration> {
    public static void main(String[] args) throws Exception {
        for(String s: args)
            System.out.println(s + "!");
        new DiretorioApplication().run(args);
    }

    @Override
    public String getName() { return "Hello"; }

    @Override
    public void initialize(Bootstrap<DiretorioConfiguration> bootstrap) { }

    @Override
    public void run(DiretorioConfiguration configuration,
                    Environment environment) {
        environment.jersey().register(
            new HelloResource(configuration.template, configuration.defaultName)
        );
        environment.jersey().register(
                new LeilaoResource()
        );
        environment.jersey().register(
                new EmissaoResource()
        );
        environment.jersey().register(
                new ExchangeResource()
        );
        environment.healthChecks().register("template",
            new TemplateHealthCheck(configuration.template));
    }

}

