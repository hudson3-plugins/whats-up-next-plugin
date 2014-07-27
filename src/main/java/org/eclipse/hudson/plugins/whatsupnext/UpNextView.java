/*
 * Copyright (c) 2014 Henrik Lynggaard Hansen
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Henrik Lynggaard Hansen - initial code
 */
package org.eclipse.hudson.plugins.whatsupnext;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ListView;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import hudson.security.ACL;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import org.hudsonci.service.SecurityService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.quartz.CronExpression;

public class UpNextView extends ListView {

    protected static final int SECOND = 0;
    protected static final int MINUTE = 1;
    protected static final int HOUR = 2;
    protected static final int DAY_OF_MONTH = 3;
    protected static final int MONTH = 4;
    protected static final int DAY_OF_WEEK = 5;
    protected static final int YEAR = 6;

    private transient SecurityService security;

    @DataBoundConstructor
    public UpNextView(String name) {
        super(name);
    }

    @Inject
    public void setSecurity(final SecurityService security) {
        this.security = security;
    }

    public SecurityService getSecurity() {

        return security;
    }

    @Override
    public synchronized List<TopLevelItem> getItems() {

//        List<TopLevelItem> names = this.getSecurity().callAs2(ACL.SYSTEM, new Callable<List<TopLevelItem>>() {
//            public List<TopLevelItem> call() {
//                List<TopLevelItem> items = new ArrayList<TopLevelItem>();
//                items.addAll(Hudson.getInstance().getItems());
//                return items;
//            }
//        });
        return Hudson.getInstance().getItems();
    }

    public List<JobEntry> getTimerTriggered() {
        List<JobEntry> result = new ArrayList<JobEntry>();
        Date now = new Date();
        for (TopLevelItem item : getItems()) {
            if (item instanceof AbstractProject) {
                AbstractProject project = (AbstractProject) item;
                TimerTrigger trigger = (TimerTrigger) project.getTrigger(TimerTrigger.class);
                if (trigger != null) {
                    JobEntry e = new JobEntry();
                    
                    e.setCrontabSpec(trigger.getSpec().replaceAll("\n", "<br />"));
                    e.setJobName(project.getDisplayName());
                    e.setNextTime(fixSpec(trigger.getSpec(), now));
                    e.setIconColor(project.getIconColor());
                    e.setUrl(project.getUrl());
                    Run lastCompletedBuild = project.getLastCompletedBuild();
                    if (lastCompletedBuild != null) {
                        e.setLastRun(lastCompletedBuild.getTime());
                    }
                    setState(e,project);
                    if (e.getNextTime() != null) {
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }

    public List<JobEntry> getScmTriggered() {
        List<JobEntry> result = new ArrayList<JobEntry>();
        Date now = new Date();
        for (TopLevelItem item : getItems()) {
            if (item instanceof AbstractProject) {
                AbstractProject project = (AbstractProject) item;
                SCMTrigger trigger = (SCMTrigger) project.getTrigger(SCMTrigger.class);
                if (trigger != null) {
                    JobEntry e = new JobEntry();
                    e.setCrontabSpec(trigger.getSpec());
                    e.setJobName(project.getDisplayName());
                    e.setNextTime(fixSpec(trigger.getSpec(), now));
                    e.setIconColor(project.getIconColor());
                    e.setUrl(project.getUrl());
                    Run lastCompletedBuild = project.getLastCompletedBuild();
                    if (lastCompletedBuild != null) {
                        e.setLastRun(lastCompletedBuild.getTime());
                    }                    
                    setState(e,project);
                    if (e.getNextTime() != null) {
                        result.add(e);
                    }
                }
            }
        }
        return result;
    }

    private void setState(JobEntry e, AbstractProject project) {
        boolean stateRunning = project.isBuilding();
        boolean stateinQueue = project.isInQueue();

        e.setState("Idle");

        if (stateRunning) {
            e.setState("Running");
        }
        if (stateinQueue) {
            e.setState("Queued");
        }
        if (stateRunning && stateinQueue) {
            e.setState("Running and next in queue");
        }
    }

    private Date fixSpec(String spec, Date now) {
        Date result = null;

        String[] lines = spec.split("\n");
        for (String line : lines) {
            if (line.startsWith("#")) {
                continue;
            }

            Date lineDate = fixSpecLine(line, now);
            if (lineDate == null) {
                continue;
            }
            if (result != null) {
                result = lineDate.before(result) ? lineDate : result;
            } else {
                result = lineDate;
            }

        }
        return result;
    }

    private Date fixSpecLine(String spec, Date now) {
        // '@yearly', '@annually', '@monthly', '@weekly', '@daily', '@midnight', and '@hourly' 
        if (spec.trim().equalsIgnoreCase("@yearly") || spec.trim().equalsIgnoreCase("@annually")) {
            return calculateNextTime("0 0 0 1 1 *", now);
        }
        if (spec.trim().equalsIgnoreCase("@monthly")) {
            return calculateNextTime("0 0 0 1 * *", now);
        }
        if (spec.trim().equalsIgnoreCase("@weekly")) {
            return calculateNextTime("0 0 0 * * 0", now);
        }
        if (spec.trim().equalsIgnoreCase("@daily") || spec.trim().equalsIgnoreCase("@midnight")) {
            return calculateNextTime("0 0 0 * * *", now);
        }
        if (spec.trim().equalsIgnoreCase("@hourly")) {
            return calculateNextTime("0 0 * * * *", now);
        }
        spec = "0 " + spec;
        String[] values = spec.split(" +");

        boolean domMatters = false;
        boolean dowMatters = false;
        if (!values[DAY_OF_MONTH].trim().equals("*") && !values[DAY_OF_MONTH].trim().equals("?")) {
            // something is specified in DAY_OF_MONTH
            values[DAY_OF_WEEK] = "?";
            domMatters = true;
        }

        if (!values[DAY_OF_WEEK].trim().equals("*") && !values[DAY_OF_WEEK].trim().equals("?")) {
            // something is specified in DAY_OF_MONTH
            values[DAY_OF_MONTH] = "?";
            dowMatters = true;
            //System.err.println("DAY of WEEK  : " + values[DAY_OF_WEEK]);
            values[DAY_OF_WEEK] = fixDayOfWeek(values[DAY_OF_WEEK]);
            //System.err.println("DAY of WEEK 2: " + values[DAY_OF_WEEK]);
        }
        if (!domMatters && !dowMatters) {
            values[DAY_OF_WEEK] = "?";
        }

        StringBuilder sb = new StringBuilder("0 ");
        sb.append(values[MINUTE]).append(" ");
        sb.append(values[HOUR]).append(" ");
        sb.append(values[DAY_OF_MONTH]).append(" ");
        sb.append(values[MONTH]).append(" ");
        sb.append(values[DAY_OF_WEEK]).append(" ");
        if (values.length == YEAR + 1) {
            sb.append(values[YEAR]).append(" ");
        }
        return calculateNextTime(sb.toString(), now);
    }

    private String fixDayOfWeek(String spec) {
        if (spec.contains("-")) {
            String[] range = spec.split("-");
            String result = "" + (Integer.parseInt(range[0]) +1);
            result = result + "-";
            result = result + Math.min(7,(Integer.parseInt(range[1]) +1));
            return result;
        }
        String[] values = spec.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<values.length; i++) {
            if (i>0) {
                sb.append(",");
            }
            sb.append(Math.min(7,(Integer.parseInt(values[i])+1)));
        }
        return sb.toString();
    }
    
    private Date calculateNextTime(String spec, Date now) {
        CronExpression exp;
        try {
            exp = new CronExpression(spec);
        } catch (ParseException ex) {
            return null;
        }
        return exp.getNextValidTimeAfter(now);
    }

    @Extension
    public static final class UpNextDescriptor extends ViewDescriptor {

        @Override
        public String getDisplayName() {
            return "What's up next";
        }
    }
}
