[![Build Status](https://travis-ci.org/jurajzachar/typesafe-config-rest.svg?branch=master)](https://travis-ci.org/jurajzachar/typesafe-config-rest)

# typesafe-config-rest
Serving Typesafe Config as REST service.

## Dependecies

  > Vert.X

  > Typesafe Config


## 1. Write your Typesafe Config
Typesafe Config brings sanity to application configuration. It's HOCON (Human Optimized Object Configuration Notation) and also supports plain JSON. For more information and overview of all features see [Typesafe Config project page](https://github.com/typesafehub/config).

A sample __application.conf__ might look like this:

    my.typesafe.config {
    	include "westeros"
    	local {
    		foo = ${my.typesafe.config.westeros.davos}
    		bar = "123?>:<foo"
    	}
    }

And referenced  __westeros.conf__:

    westeros {
    	jamie = lanister
    	daenerys = targaryen
    	theon = greyjoy
    	tyrion = lannister
    	areo = hotah
    	davos = seaworth
    	arianne = martell
    }

## 2. Deploy HTTP Server

You can embed __TypesafeConfigHttpServerVerticle__ in your Java code. For more info on what is Vert.X and how to use it go [here](http://vertx.io/).

    config.put(ROOT_CTX_CNFK, "test-config");
    config.put(PATH_TO_CONFIG_CNFK, "/path/to/my/application.conf");
    DeploymentOptions depOpts = new DeploymentOptions();
    depOpts.setConfig(config);
    vertx.deployVerticle(new TypesafeConfigHttpServerVerticle(), depOpts);

Alternatively, you can run this service as standalone java process:

      java -Dhost=myserver -Dport=8123 Dconfig=/path/to/my/application.conf -jar typesafe-config-rest-$version-jarWithDependencies.jar

## 3. GET my config!

Get the whole config.

    curl -X GET localhost:8123/test-config/my

    {
      "key" : "my",
      "value" : {
        "typesafe" : {
          "config" : {
            "westeros" : {
              "jamie" : "lanister",
              "davos" : "seaworth",
              "theon" : "greyjoy",
              "daenerys" : "targaryen",
              "areo" : "hotah",
              "arianne" : "martell",
              "tyrion" : "lannister"
            },
            "local" : {
              "bar" : "123?><foo",
              "foo" : "seaworth"
            }
          }
        }
      }
    }

Or dig deeper.

    curl -X GET localhost:8123/test-config/my/typesafe/config/westeros/tyrion

    {
      "key" : "my.typesafe.config.westeros.tyrion",
      "value" : "lannister"
    }

## 4. Update config and reload
You can update your config at any time without restarting the server Verticle. All you have to do is to post a reload request.

    curl -X POST localhost:8123/test-config/reload

    {
      "key" : "reload",
      "value" : "Config '/path/to/my/config/application.conf' reloaded sucessfully at 2017-05-04T23:03:52.628"
    }
