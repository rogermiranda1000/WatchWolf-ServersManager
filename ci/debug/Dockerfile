FROM openjdk:8
ARG WSL_MODE=default
ENV WSL_MODE=${WSL_MODE}
ENTRYPOINT ["/bin/bash", "-c"]
CMD ["echo \"WSL_MODE is : $WSL_MODE\" ; echo \"MACHINE_IP is : $MACHINE_IP\" ; echo \"PUBLIC_IP is : $PUBLIC_IP\""]