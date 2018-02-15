node ('docker') {
    stage('test project') {
        checkout scm
        sh 'chmod +x ./gradlew'
        sh './gradlew clean test'
        junit healthScaleFactor: 100.0, testResults: '**/test-results/**/*.xml'
     }

    stage('build docker') {
        withCredentials([usernamePassword(credentialsId: 'docker', passwordVariable: 'password', usernameVariable: 'user')]) {
            sh "docker login -u $user -p $password"
            checkout scm
            sh 'chmod +x ./gradlew'
            sh "./gradlew dockerPushProjectVersion dockerPushLatest generateK8sFile -Pv=$BUILD_NUMBER"
            archiveArtifacts 'build/deployment.yaml'
            stash includes: 'build/deployment.yaml', name: 'deployment.yaml'
        }
    }
}
/*
node ('kubectl') {
    stage('deploy') {
        checkout scm
        unstash 'deployment.yaml'
        sh 'kubectl -n leanforge apply -f build/deployment.yaml'
    }
}
*/