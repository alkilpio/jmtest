node{

    stage('Get') {
        echo 'Stage <Get>'
        sh 'whoami'
        checkoutFromGithubToSubfolder('jmtest', MASTER_BRANCH)
    }

    stage('Modify'){
        modifyReadmeMd('master')
    }

    stage('UpdateAndCommit'){
        echo 'Stage <UpdateAndCommit>'

        commitBranch('master')
    }
    stage('GitPush'){
        echo 'Stage <GitPush>'
        gitPushToBranch('master')
    }

}

//!================================================================================================================

def checkoutFromGithubToSubfolder(repositoryName, def branch = 'master', def clean = true) {
    def extensions = [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${repositoryName}"],
    [$class: 'UserIdentity', name: "${GIT_REPO_OWNER}"],
    [$class: 'ScmName', name: "${GIT_REPO_OWNER}"]
    ]
    if (clean) {
        extensions.push([$class: 'WipeWorkspace']) //CleanBeforeCheckout
    }
    checkout([$class                         : 'GitSCM', branches: [[name: "*/${MASTER_BRANCH}"], [name: "*/${branch}"]],
        doGenerateSubmoduleConfigurations    : false, submoduleCfg: [],
        userRemoteConfigs                    : [[url: "https://github.com/${GIT_REPO_OWNER}/${repositoryName}.git"]],
        extensions                           : extensions
    ])
}

void commitBranch(branchName) {
    dir('jmtest') {
        sh "git checkout ${branchName}"
        sh "git config user.name ${GIT_REPO_OWNER}"
        sh "git config user.email ${GIT_REPO_OWNER}@jenkins"
        sh "git commit -m '${branchName}' || true"
    }
}

void gitPushToBranch(branchName) {
    dir('jmtest') {

        sshagent(credentials: ['${GITHUB_SSH_CREDENTIALS_ID}']) {
            //we need this for git push to work, cause origin is now https:// not git@                
            sh "git remote set-url origin git@github.com:${GIT_REPO_OWNER}/jmtest.git"
            sh "git checkout ${branchName}"
            sh("git push -u origin ${branchName}")
        }

    }
}

void modifyReadmeMd(branchName) {
    dir('jmtest') {
        sh "git checkout ${branchName}"
        def fileContent = readFile 'README.md'
        writeFile file: 'README.md', text: "${fileContent}\n${GIT_REPO_OWNER}"
        sh "git add README.md"
    }
}