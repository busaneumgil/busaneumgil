import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.Credentials
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl

def jenkins = Jenkins.get()
def store = SystemCredentialsProvider.getInstance().getStore()

def removeIfExists = { String id ->
    def existing = CredentialsProvider.lookupCredentials(
        Credentials.class,
        jenkins,
        null,
        null
    ).find { it.id == id }

    if (existing != null) {
        store.removeCredentials(Domain.global(), existing)
    }
}

def upsertStringCredential = { String id, String description, String value ->
    if (value == null || value.trim().isEmpty()) {
        println("[e102] skip credential ${id}: blank value")
        return
    }

    removeIfExists(id)
    def credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        id,
        description,
        Secret.fromString(value.trim())
    )
    store.addCredentials(Domain.global(), credential)
    println("[e102] upserted string credential ${id}")
}

def upsertSshKeyCredential = { String id, String description, String username, String keyPath ->
    File key = new File(keyPath)
    if (!key.isFile()) {
        println("[e102] skip credential ${id}: missing key ${keyPath}")
        return
    }

    removeIfExists(id)
    def credential = new BasicSSHUserPrivateKey(
        CredentialsScope.GLOBAL,
        id,
        username,
        new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(key.text),
        '',
        description
    )
    store.addCredentials(Domain.global(), credential)
    println("[e102] upserted SSH key credential ${id}")
}

removeIfExists('e102-dev-env-file')
removeIfExists('e102-prod-env-file')
println('[e102] removed deprecated env-file credentials when present')

upsertStringCredential(
    'e102-s2-host',
    'E102 S2 production SSH host',
    System.getenv('E102_S2_HOST') ?: '43.201.198.214'
)

upsertSshKeyCredential(
    'e102-s2-ssh-key',
    'E102 S2 production SSH private key',
    System.getenv('E102_S2_USER') ?: 'ubuntu',
    '/var/jenkins_home/prod-secrets/busan-eumgil-S2.pem'
)

upsertStringCredential(
    'e102-mattermost-webhook-url',
    'E102 Mattermost incoming webhook URL',
    System.getenv('MATTERMOST_WEBHOOK_URL')
)

SystemCredentialsProvider.getInstance().save()
