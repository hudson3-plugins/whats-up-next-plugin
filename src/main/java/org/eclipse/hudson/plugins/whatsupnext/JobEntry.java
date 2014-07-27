/*
 * Copyright (c) 2014 Henrik Lynggaard Hansen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Henrik Lynggaard Hansen - initial code
 */

package org.eclipse.hudson.plugins.whatsupnext;

import hudson.model.BallColor;
import java.util.Date;

/**
 *
 * @author henrik
 */
public class JobEntry {
   
    private String jobName;
    private Date nextTime;
    private String url;
    private String crontabSpec;
    private Date lastRun;
    private BallColor iconColor;
    private String state;

    public BallColor getIconColor() {
        return iconColor;
    }
    
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Date getNextTime() {
        return nextTime;
    }

    public void setNextTime(Date nextTime) {
        this.nextTime = nextTime;
    }


    public String getCrontabSpec() {
        return crontabSpec;
    }

    public void setCrontabSpec(String crontabSpec) {
        this.crontabSpec = crontabSpec;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.jobName != null ? this.jobName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JobEntry other = (JobEntry) obj;
        if ((this.jobName == null) ? (other.jobName != null) : !this.jobName.equals(other.jobName)) {
            return false;
        }
        return true;
    }


    void setIconColor(BallColor iconColor) {
        this.iconColor = iconColor;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getLastRun() {
        return lastRun;
    }

    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    
    

    
    
    
}
