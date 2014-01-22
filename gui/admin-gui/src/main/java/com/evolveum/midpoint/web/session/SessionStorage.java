/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.session;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SessionStorage implements Serializable {

    /**
     * place to store "previous page" for back button
     */
    private Class<? extends WebPage> previousPage;
    /**
     * place to store "previous page" parameters for back button
     */
    private PageParameters previousPageParams;

    /**
     * place to store informations in session for "configuration" pages
     */
    private ConfigurationStorage configuration;
    /**
     * Store session information for "users" pages
     */
    private UsersStorage users;
    private ReportsStorage reports;

    public Class<? extends WebPage> getPreviousPage() {
        return previousPage;
    }

    public void setPreviousPage(Class<? extends WebPage> previousPage) {
        this.previousPage = previousPage;
    }

    public PageParameters getPreviousPageParams() {
        return previousPageParams;
    }

    public void setPreviousPageParams(PageParameters previousPageParams) {
        this.previousPageParams = previousPageParams;
    }

    public ConfigurationStorage getConfiguration() {
        if (configuration == null) {
            configuration = new ConfigurationStorage();
        }
        return configuration;
    }

    public UsersStorage getUsers() {
        if (users == null) {
            users = new UsersStorage();
        }
        return users;
    }

    public ReportsStorage getReports(){
        if(reports == null){
            reports = new ReportsStorage();
        }
        return reports;
    }
}
