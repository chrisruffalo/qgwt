# QGWT - Quarkus GWT Extension

## Description
After seeing [Quinoa](https://github.com/quarkiverse/quarkus-quinoa) the idea came to me that something similar could be 
done with GWT's CodeServer mode. After a lot of messing around it finally works and you can use the CodeServer to
serve GWT resources while using Quarkus to serve the rest.

## ToDo
- ~~Get CodeServer working as a build step~~
- ~~Enable live-reload on the CodeServer~~
- ~~Transparently proxy requests to the CodeServer~~
- Get GWTc to work as part of build process for jvm/native images