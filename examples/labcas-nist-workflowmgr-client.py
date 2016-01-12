# Example of python script to execute a NIST workflow

import xmlrpclib
import time
import solr

# global objects
verbose = False
workflowManagerServerProxy = xmlrpclib.ServerProxy('http://localhost:9001/', verbose=verbose)
fileManaferServerProxy = xmlrpclib.ServerProxy('http://localhost:9000/', verbose=verbose)
solrServerProxy = solr.SolrConnection("http://localhost:8983/solr/oodt-fm")

def waitForCompletion(wInstId):
    ''' Monitors a workflow instance until it completes.'''
    
    # wait for the server to instantiate this workflow before querying it
    time.sleep(2) 

    # now use the workflow instance id to check for status, wait until completed
    running_status  = ['CREATED', 'QUEUED', 'STARTED', 'PAUSED']
    pge_task_status = ['STAGING INPUT', 'BUILDING CONFIG FILE', 'PGE EXEC', 'CRAWLING']
    finished_status = ['FINISHED', 'ERROR']
    while (True):
        response = workflowManagerServerProxy.workflowmgr.getWorkflowInstanceById(wInstId)
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
    
    # submit workflow
    # /wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName nist --metaData --key pi Johns --key instrument trombone --key lab LAB01 --key date 20160101 --key mirnadatadir nist/LAB01/20160101
    wInstId = workflowManagerServerProxy.workflowmgr.executeDynamicWorkflow( ['urn:edrn:NistInitTask','urn:edrn:NistConvertTask','urn:edrn:NistExecuteTask'], 
                                                                             {'pi':'Johns', 
                                                                              'instrument':'trumpet', 
                                                                              'lab':'LAB01','date':'20160101', 
                                                                              'mirnadatadir':'nist/LAB01/20160101' } )

    # monitor workflow instance
    waitForCompletion(wInstId)
