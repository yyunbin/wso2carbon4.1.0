/**
 *  Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.ntask.core.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.OperableTrigger;
import org.wso2.carbon.ntask.common.TaskConstants;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.common.TaskException.Code;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.ntask.core.TaskRepository;
import org.wso2.carbon.ntask.core.TaskUtils;
import org.wso2.carbon.ntask.core.TaskInfo.TriggerInfo;
import org.wso2.carbon.ntask.core.internal.TasksDSComponent;

/**
 * This class represents an abstract class implementation of TaskManager based on Quartz Scheduler.
 * @see TaskManager
 */
public abstract class AbstractQuartzTaskManager implements TaskManager {

	private static final Log log = LogFactory.getLog(AbstractQuartzTaskManager.class);
	
	private TaskRepository taskRepository;
	
	private Scheduler scheduler;
	
	public AbstractQuartzTaskManager(TaskRepository taskRepository) throws TaskException {
		this.taskRepository = taskRepository;
	    this.scheduler = TasksDSComponent.getScheduler(); 
	}

	public TaskRepository getTaskRepository() {
		return taskRepository;
	}
	
	protected Scheduler getScheduler() {
		return scheduler;
	}
	
	public int getTenantId() {
		return this.getTaskRepository().getTenantId();
	}
	
	public String getTaskType() {
		return this.getTaskRepository().getTasksType();
	}
	
	protected TaskState getLocalTaskState(String taskName) throws TaskException {
		String taskGroup = this.getTenantTaskGroup();
		try {
			return triggerStateToTaskState(this.getScheduler().getTriggerState(
					new TriggerKey(taskName, taskGroup)));
		} catch (SchedulerException e) {
			throw new TaskException("Error in checking state of the task with the name: "
					+ taskName, Code.UNKNOWN, e);
		}
	}
	
	protected Map<String, TaskState> getAllLocalTaskStates() throws TaskException {
		try {
			Set<TriggerKey> keys = this.getScheduler().getTriggerKeys(
					GroupMatcher.triggerGroupEquals(this.getTenantTaskGroup()));
			Map<String, TaskState> states = new HashMap<String, TaskManager.TaskState>();
			for (TriggerKey key : keys) {
				states.put(key.getName(), triggerStateToTaskState(
						this.getScheduler().getTriggerState(
					    new TriggerKey(key.getName(), key.getGroup()))));
			}
			return states;
		} catch (SchedulerException e) {
			throw new TaskException("Error in retrieving task states", Code.UNKNOWN, e);
		}
	}
	
	protected void registerLocalTask(TaskInfo taskInfo) throws TaskException {
		this.getTaskRepository().addTask(taskInfo);
	}
	
	private TaskState triggerStateToTaskState(TriggerState triggerState) {
		if (triggerState == TriggerState.NONE) {
			return TaskState.NONE;
		} else if (triggerState == TriggerState.PAUSED) {
			return TaskState.PAUSED;
		} else if (triggerState == TriggerState.COMPLETE) {
			return TaskState.FINISHED;
		} else if (triggerState == TriggerState.ERROR) {
			return TaskState.ERROR;
		} else if (triggerState == TriggerState.NORMAL) {
			return TaskState.NORMAL;
		} else if (triggerState == TriggerState.BLOCKED) {
			return TaskState.BLOCKED;
		} else {
			return TaskState.UNKNOWN;
		}
	}
	
	protected synchronized boolean deleteLocalTask(String taskName, boolean removeRegistration) throws TaskException {
		String taskGroup = this.getTenantTaskGroup();
		boolean result = false;
		try {
		    result = this.getScheduler().deleteJob(new JobKey(taskName, taskGroup));
		} catch (SchedulerException e) {
			throw new TaskException("Error in deleting task with name: " + taskName,
					Code.UNKNOWN, e);
		}
		if (removeRegistration) {
			result &= this.getTaskRepository().deleteTask(taskName);
		}
		return result;
	}
	
	protected synchronized void pauseLocalTask(String taskName) throws TaskException {
		String taskGroup = this.getTenantTaskGroup();
		try {
		    this.getScheduler().pauseJob(new JobKey(taskName, taskGroup));
		} catch (SchedulerException e) {
			throw new TaskException("Error in pausing task with name: " + taskName,
					Code.UNKNOWN, e);
		}
	}
		
	private String getTenantTaskGroup() {
		return "TENANT_" + this.getTenantId() + "_TYPE_" + this.getTaskType();
	}
	
	private JobDataMap getJobDataMapFromTaskInfo(TaskInfo taskInfo) {
		JobDataMap dataMap = new JobDataMap();
		dataMap.put(TaskConstants.TASK_CLASS_NAME, taskInfo.getTaskClass());
		dataMap.put(TaskConstants.TASK_PROPERTIES, taskInfo.getProperties());
		return dataMap;
	}
	
	protected synchronized void scheduleLocalAllTasks() throws TaskException {
		List<TaskInfo> tasks = this.getTaskRepository().getAllTasks();
		for (TaskInfo task : tasks) {
			try {
			    this.scheduleTask(task.getName());
			} catch (Exception e) {
				log.error("Error in scheduling task: " + e.getMessage(), e);
			}
		}
	}
	
	protected synchronized void scheduleLocalTask(String taskName) throws TaskException {
		boolean paused = TaskUtils.isTaskPaused(this.getTaskRepository(), taskName);
		this.scheduleLocalTask(taskName, paused);
	}
	
	protected synchronized void scheduleLocalTask(String taskName, 
			boolean paused) throws TaskException {
		TaskInfo taskInfo = this.getTaskRepository().getTask(taskName);
		String taskGroup = this.getTenantTaskGroup();
		if (taskInfo == null) {
			throw new TaskException("Non-existing task for scheduling with name: " + taskName,
					Code.NO_TASK_EXISTS);
		}
		if (this.containsLocalTask(taskName, taskGroup)) {
			throw new TaskException("The task with name: " + taskName + ", already started.",
					Code.TASK_ALREADY_STARTED);
		}
		Class<? extends Job> jobClass = taskInfo.getTriggerInfo().isDisallowConcurrentExecution() ? 
				NonConcurrentTaskQuartzJobAdapter.class : TaskQuartzJobAdapter.class;
		JobDetail job = JobBuilder.newJob(jobClass).withIdentity(taskName, taskGroup).usingJobData(
				this.getJobDataMapFromTaskInfo(taskInfo)).build();		
		Trigger trigger = this.getTriggerFromInfo(taskName, taskGroup, taskInfo.getTriggerInfo());
		try {
			this.getScheduler().scheduleJob(job, trigger);
			if (paused) {
				this.getScheduler().pauseJob(job.getKey());
			}
			log.info("Task scheduled: [" + this.getTenantId() + 
					"][" + this.getTaskType() + "][" + taskName + "]");
		} catch (SchedulerException e) {
			throw new TaskException("Error in scheduling task with name: " + taskName,
					Code.UNKNOWN, e);
		}
	}
	
	private Trigger getTriggerFromInfo(String taskName, String taskGroup,
			TriggerInfo triggerInfo) throws TaskException {
		TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(
				taskName, taskGroup);
		if (triggerInfo.getStartTime() == null) {
			triggerBuilder = triggerBuilder.startNow();
		} else {
			triggerBuilder = triggerBuilder.startAt(triggerInfo.getStartTime());
		}
		if (triggerInfo.getEndTime() != null) {
			triggerBuilder.endAt(triggerInfo.getEndTime());
		}
		Trigger trigger;
		if (triggerInfo.getCronExpression() != null) {
			trigger = triggerBuilder.withSchedule(this.getCronScheduleBuilder(
					triggerInfo)).build();
		} else {
			if (triggerInfo.getRepeatCount() == 0) {
				/* only once executed */
				trigger = triggerBuilder.build();
			} else {
			    trigger = triggerBuilder.withSchedule(this.getSimpleScheduleBuilder(
					triggerInfo)).build();
			}
		}
		return trigger;
	}
	
	protected synchronized void rescheduleLocalTask(String taskName) throws TaskException {
		String taskGroup = this.getTenantTaskGroup();
		TaskInfo taskInfo = this.getTaskRepository().getTask(taskName);
		Trigger trigger = this.getTriggerFromInfo(taskName, taskGroup, taskInfo.getTriggerInfo());
		try {
			boolean paused = TaskUtils.isTaskPaused(this.getTaskRepository(), taskName);
			Date resultDate = this.getScheduler().rescheduleJob(
					new TriggerKey(taskName, taskGroup), trigger);
			if (resultDate == null) {
				/* do normal schedule */
				this.scheduleLocalTask(taskName, paused);
			} else if (paused) {
				this.pauseLocalTask(taskName);
			}
		} catch (SchedulerException e) {
			throw new TaskException("Error in rescheduling task with name: " + taskName,
					Code.UNKNOWN, e);
		}
	}
	
	protected synchronized void resumeLocalTask(String taskName) throws TaskException {
		String taskGroup = this.getTenantTaskGroup();
		if (!this.containsLocalTask(taskName, taskGroup)) {
			throw new TaskException("Non-existing task for resuming with name: " + taskName,
					Code.NO_TASK_EXISTS);
		}
		try {
			Trigger trigger = this.getScheduler().getTrigger(new TriggerKey(taskName, taskGroup));
			if (trigger instanceof OperableTrigger) {
				((OperableTrigger) trigger).setNextFireTime(
						((OperableTrigger) trigger).getFireTimeAfter(null));
			}
			this.getScheduler().resumeJob(new JobKey(taskName, taskGroup));
		} catch (SchedulerException e) {
			throw new TaskException("Error in resuming task with name: " + taskName,
					Code.UNKNOWN, e);
		}
	}
	
	protected synchronized boolean isLocalTaskScheduled(String taskName) throws TaskException {
		String taskGroup = this.getTenantTaskGroup();
		return this.containsLocalTask(taskName, taskGroup);
	}
	
	protected List<TaskInfo> getAllLocalScheduledTasks() throws TaskException {
		List<TaskInfo> tasks = this.getTaskRepository().getAllTasks();
		List<TaskInfo> result = new ArrayList<TaskInfo>();
		for (TaskInfo taskInfo : tasks) {
			if (this.isLocalTaskScheduled(taskInfo.getName())) {
				result.add(taskInfo);
			}
		}
		return result;
	}
	
	private boolean containsLocalTask(String taskName, String taskGroup) throws TaskException {
		try {
		    return this.getScheduler().checkExists(new JobKey(taskName, taskGroup));
		} catch (SchedulerException e) {
			throw new TaskException("Error in retrieving task details", Code.UNKNOWN, e);
		}
	}
	
	private CronScheduleBuilder getCronScheduleBuilder(TriggerInfo triggerInfo) 
	            throws TaskException {
		CronScheduleBuilder cb = CronScheduleBuilder.cronSchedule(triggerInfo.getCronExpression());
		cb = this.handleCronScheduleMisfirePolicy(triggerInfo, cb);
		return cb;
	}
	
	private CronScheduleBuilder handleCronScheduleMisfirePolicy(TriggerInfo triggerInfo, 
			CronScheduleBuilder cb) throws TaskException {
		switch (triggerInfo.getMisfirePolicy()) {
		case DEFAULT:
			return cb;
		case IGNORE_MISFIRES:
			return cb.withMisfireHandlingInstructionIgnoreMisfires();
		case FIRE_AND_PROCEED:
			return cb.withMisfireHandlingInstructionFireAndProceed();
		case DO_NOTHING:
			return cb.withMisfireHandlingInstructionDoNothing();
		default:
			throw new TaskException("The task misfire policy '" + triggerInfo.getMisfirePolicy() +
					"' cannot be used in cron schedule tasks", Code.CONFIG_ERROR);
		}
	}
	
	private SimpleScheduleBuilder getSimpleScheduleBuilder(TriggerInfo triggerInfo) 
	            throws TaskException {
		SimpleScheduleBuilder scheduleBuilder = null;
		if (triggerInfo.getRepeatCount() == -1) {
			scheduleBuilder = SimpleScheduleBuilder.simpleSchedule().repeatForever();
		} else if (triggerInfo.getRepeatCount() > 0) {
			scheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withRepeatCount(
					triggerInfo.getRepeatCount());
		}
		scheduleBuilder = scheduleBuilder.withIntervalInMilliseconds(
				triggerInfo.getIntervalMillis());
		scheduleBuilder = this.handleSimpleScheduleMisfirePolicy(triggerInfo, scheduleBuilder);
		return scheduleBuilder;
	}
	
	private SimpleScheduleBuilder handleSimpleScheduleMisfirePolicy(TriggerInfo triggerInfo, 
			SimpleScheduleBuilder sb) throws TaskException {
		switch (triggerInfo.getMisfirePolicy()) {
		case DEFAULT:
			return sb;
		case FIRE_NOW:
			return sb.withMisfireHandlingInstructionFireNow();
		case IGNORE_MISFIRES:
			return sb.withMisfireHandlingInstructionIgnoreMisfires();
		case NEXT_WITH_EXISTING_COUNT:
			return sb.withMisfireHandlingInstructionNextWithExistingCount();
		case NEXT_WITH_REMAINING_COUNT:
			return sb.withMisfireHandlingInstructionNextWithRemainingCount();
		case NOW_WITH_EXISTING_COUNT:
			return sb.withMisfireHandlingInstructionNowWithExistingCount();
		case NOW_WITH_REMAINING_COUNT:
			return sb.withMisfireHandlingInstructionNowWithRemainingCount();
		default:
			throw new TaskException("The task misfire policy '" + triggerInfo.getMisfirePolicy() +
					"' cannot be used in simple schedule tasks", Code.CONFIG_ERROR);
		}
	}
	
}
