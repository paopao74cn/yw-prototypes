package org.yesworkflow.recon;

import java.util.Collection;

import org.yesworkflow.config.Configurable;
import org.yesworkflow.data.UriTemplate;

public interface ResourceFinder extends Configurable {
    
    enum ResourceRole { INPUT, OUTPUT, INPUT_OR_OUTPUT };
    
    Collection<String> findResources(String baseUri, UriTemplate uriTemplate, ResourceRole role);
}
