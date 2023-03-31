#!/bin/bash

cd .. && mvn clean install && cd - && mvn clean compile quarkus:dev