# typesafe-config-rest
Serving Typesafe Config as REST service.

## Dependecies

  > Vert.X

  > Typesafe Config


## 1. Write your Typesafe Config
Typesafe Config brings sanity application configuration. It's HOCON (Human Optimized Object Configuration Notation) and also supports plain JSON. For more information see [Typesafe Config project page](https://github.com/typesafehub/config).

A sample config might look like this.

    my.typesafe.config {
    	local {
    		foo = 1
    		bar = 2
    	}
    	westeros {
    		jamie = lanister
    		daenerys = targaryen
    	}
    }

## 2. Deploy HTTP Server

Deploy __TypesafeConfigHttpServerVerticle__. For more info on what is Vert.X and how to use it go [here](http://vertx.io/).

    config.put(ROOT_CTX_CNFK, "test-config");
    config.put(PATH_TO_CONFIG_CNFK, currentWorkDir + "/src/test/resources/application.conf");
    DeploymentOptions depOpts = new DeploymentOptions();
    depOpts.setConfig(config);
    vertx.deployVerticle(new TypesafeConfigHttpServerVerticle(), depOpts);

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
              "daenerys" : "targaryen"
            },
            "local" : {
              "bar" : 2,
              "foo" : 1
            }
          }
        }
      }
    }

Or dig deeper.

    curl -X GET localhost:8123/test-config/my/typesafe/config/local

    {
      "key" : "my.typesafe.config.local",
      "value" : {
        "bar" : 2,
        "foo" : 1
      }
    }

## 4. Update config and reload
You can update your config at any time without restarting the server Verticle. All you have to do is to post a reload request.

    curl -X POST localhost:8123/test-config/reload

    {
      "key" : "reload",
      "value" : "Config '/path/to/my/config/application.conf' reloaded sucessfully at 2017-05-04T23:03:52.628"
    }
