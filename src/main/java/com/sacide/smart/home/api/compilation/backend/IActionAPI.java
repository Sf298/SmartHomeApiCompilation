/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation.backend;

import java.util.Collection;

/**
 *
 * @author saud
 */
public interface IActionAPI {
    
    /**
     * Finds all available devices and stores them in a local cache.
     * @return returns all devices stored in the local cache.
     */
    public Collection<Device> discoverDevices();
    
}
