FROM openjdk:8

ARG WSL_MODE=default
ENV WSL_MODE=${WSL_MODE}

RUN mkdir /servers
ADD ServersManager.jar /servers
WORKDIR /servers

ENTRYPOINT ["/bin/bash", "-c"]
CMD ["echo \"[v] WSL_MODE: $WSL_MODE\" ; echo \"[v] MACHINE_IP: $MACHINE_IP\" ; echo \"[v] PUBLIC_IP: $PUBLIC_IP\" ; echo \"[v] PARENT_PWD: $PARENT_PWD\" ; java -cp ServersManager.jar dev/watchwolf/serversmanager/rpc/ServersManagerLocalImplementation"]