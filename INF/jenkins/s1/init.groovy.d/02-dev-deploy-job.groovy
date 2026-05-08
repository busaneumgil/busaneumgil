import jenkins.model.Jenkins
import hudson.triggers.SCMTrigger
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty

String jobName = 'e102-dev-deploy'
File marker = new File('/var/jenkins_home/.e102-dev-deploy-job-created')
File pipelineFile = new File('/usr/share/jenkins/ref/pipelines/e102-dev-deploy.Jenkinsfile')
String pipelineScript = pipelineFile.getText('UTF-8')

Jenkins j = Jenkins.get()
def job = j.getItem(jobName)
if (job == null) {
  job = j.createProject(WorkflowJob.class, jobName)
}
job.setDescription('Deploy develop branch to S1 dev Docker Compose stack. GraphHopper cache is built from PostgreSQL LineString when missing.')
job.setDefinition(new CpsFlowDefinition(pipelineScript, true))
if (job.getProperty(PipelineTriggersJobProperty.class) == null) {
  job.addProperty(new PipelineTriggersJobProperty([new SCMTrigger('H/30 * * * *')]))
}
job.save()

if (!marker.exists()) {
  job.scheduleBuild2(0)
  marker.text = new Date().toString()
  println("${jobName} configured and initial build scheduled")
} else {
  println("${jobName} configured")
}
