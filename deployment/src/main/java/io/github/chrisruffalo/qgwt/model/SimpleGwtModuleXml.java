package io.github.chrisruffalo.qgwt.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "module")
public class SimpleGwtModuleXml {

    @XmlElement(name = "inherits")
    private List<GwtInherits> inherits = new LinkedList<>();

    @XmlElement(name = "source")
    private List<GwtSource> sources = new LinkedList<>();

    @XmlElement(name = "super-source")
    private List<GwtSuperSource> superSources = new LinkedList<>();

    @XmlElement(name = "public")
    private List<GwtSuperSource> publicResources = new LinkedList<>();

    @XmlElement(name = "stylesheet")
    private List<GwtSuperSource> stylesheets = new LinkedList<>();


    public List<GwtInherits> getInherits() {
        return inherits;
    }

    public void setInherits(List<GwtInherits> inherits) {
        this.inherits = inherits;
    }

    public List<GwtSource> getSources() {
        return sources;
    }

    public void setSources(List<GwtSource> sources) {
        this.sources = sources;
    }

    public List<GwtSuperSource> getSuperSources() {
        return superSources;
    }

    public void setSuperSources(List<GwtSuperSource> superSources) {
        this.superSources = superSources;
    }

    public List<GwtSuperSource> getPublicResources() {
        return publicResources;
    }

    public void setPublicResources(List<GwtSuperSource> publicResources) {
        this.publicResources = publicResources;
    }

    public List<GwtSuperSource> getStylesheets() {
        return stylesheets;
    }

    public void setStylesheets(List<GwtSuperSource> stylesheets) {
        this.stylesheets = stylesheets;
    }
}
