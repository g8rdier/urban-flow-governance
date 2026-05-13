# Ansible Deployment

Automatisiertes Deployment via Ansible, ausgelöst durch einen server-seitigen `post-receive` Git Hook.

## Ablauf

```
git push origin main
  → post-receive Hook (Server)
    → ansible-playbook deploy.yml
      → Frontend nach Apache kopieren
      → Grails WAR bauen und nach Tomcat deployen
```

## Komponenten

### post-receive Hook

Liegt auf dem Server unter `.git/hooks/post-receive` im bare Repository. Prüft ob `main` gepusht wurde und ruft das Playbook auf.

```sh
#!/bin/sh
while read old new ref; do
  if [ "$ref" = "refs/heads/main" ]; then
    ansible-playbook /opt/ufg/deploy.yml
  fi
done
```

### Ansible Playbook (`deploy.yml`)

```yaml
- hosts: localhost
  connection: local
  tasks:
    - name: Frontend deployen
      copy:
        src: /media/sf_iu/git/elgreti/urban-flow-governance/frontend/
        dest: /media/sf_iu/daps-ss26/elgreti/webclient/

    - name: Grails WAR bauen
      command: ./gradlew war
      args:
        chdir: /media/sf_iu/git/elgreti/urban-flow-governance/ufg-app

    - name: WAR nach Tomcat deployen
      copy:
        src: /media/sf_iu/git/elgreti/urban-flow-governance/ufg-app/build/libs/elgreti-ufg.war
        dest: /var/lib/tomcat9/webapps/elgreti-ufg.war
```

## Hinweise

- Der lokale Pre-push Hook (`.githooks/pre-push`) wird mit dieser Lösung obsolet.
- Ansible muss auf dem Server installiert sein (`apt install ansible`).
- Das bare Repository auf dem Server muss beschreibbar sein für den Hook.
