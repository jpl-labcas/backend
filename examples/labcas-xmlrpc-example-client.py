#!/usr/bin/python
# Example script for submitting and monitoring a 'labcas-upload' workflow

import xmlrpclib
import time


def waitForCompletion(wInstId):
	''' Monitors a workflow instance until it completes.'''
	
	# wait for the server to instantiate this workflow before querying it
	time.sleep(1) 

	# now use the workflow instance id to check for status, wait until completed
	running_status  = ['CREATED', 'QUEUED', 'STARTED', 'PAUSED']
	pge_task_status = ['STAGING INPUT', 'BUILDING CONFIG FILE', 'PGE EXEC', 'CRAWLING']
	finished_status = ['FINISHED', 'ERROR']
	while (True):
		response = server.workflowmgr.getWorkflowInstanceById(wInstId)
	        status = response['status']
	        if status in running_status or status in pge_task_status:
	        	print 'Workflow istance=%s running with status=%s' % (wInstId, status)
	        	time.sleep(1)
		elif status in finished_status:
			print 'Workflow istance=%s ended with status=%s' % (wInstId, status)
			break
		else:
			print 'UNRECOGNIZED WORKFLOW STATUS: %s' % status
			break
	print response

if __name__ == '__main__':
	
	# connect to Workflow Manager server
	# use verbose=True to print out every request/response 
	verbose=False
	server = xmlrpclib.ServerProxy('http://localhost:9001/', verbose=verbose)
	
	# list all supported events
	#server.workflowmgr.getRegisteredEvents()
	
	# list all supported workflows
	#server.workflowmgr.getWorkflows()
	
	# 1) submit "labcas-upload" workflow
	# NOTE: currently, if you start a named workflow, the XMLRPC interface only returns True/False, not a workflow instance identifier...
	#wInstId = server.workflowmgr.handleEvent('labcas-upload', { 'Dataset':'mydata' } )
	
	# ... consequently, you must submit an equivalent dynamic workflow, which does return the workflow instance id
	wInstId1 = server.workflowmgr.executeDynamicWorkflow( ['urn:edrn:LabcasUploadInitTask','urn:edrn:LabcasUploadExecuteTask'], { 'Dataset':'mydata' } )

	# monitor workflow instance
	waitForCompletion(wInstId1)
	
	# 2) submit "labcas-update" workflow
	wInstId2 = server.workflowmgr.executeDynamicWorkflow( ['urn:edrn:LabcasUpdateTask'], { 'Dataset':'mydata' } )

	# monitor workflow instance
	waitForCompletion(wInstId2)

