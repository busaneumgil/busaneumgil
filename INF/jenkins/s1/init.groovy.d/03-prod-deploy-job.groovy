import jenkins.model.Jenkins
import hudson.triggers.SCMTrigger
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty

String jobName = 'e102-prod-deploy'
File pipelineFile = new File('/usr/share/jenkins/ref/pipelines/e102-prod-deploy.Jenkinsfile')
String pipelineScript = pipelineFile.getText('UTF-8')

Jenkins j = Jenkins.get()
def job = j.getItem(jobName)
if (job == null) {
  job = j.createProject(WorkflowJob.class, jobName)
}
job.setDescription('master 브랜치를 30분마다 Poll SCM으로 확인해 S2 prod 서버에 backend, AI, 선택적 GraphHopper graph-cache/runtime을 배포합니다.')
job.setDefinition(new CpsFlowDefinition(pipelineScript, true))
if (job.getProperty(PipelineTriggersJobProperty.class) == null) {
  job.addProperty(new PipelineTriggersJobProperty([new SCMTrigger('H/30 * * * *')]))
}
job.save()
println("${jobName} configured")
