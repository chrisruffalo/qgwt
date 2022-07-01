package io.github.chrisruffalo.qgwt.example.services;

import javax.ws.rs.Path;

@Path("/")
public class Api {

    @Path("/hello")
    public String hello() {
        return "hello";
    }

}
