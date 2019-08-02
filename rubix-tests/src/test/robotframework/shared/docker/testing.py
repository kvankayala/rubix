#!/usr/bin/env python
import sys

print ("Hello World")


str = "version: '3.7'\n" \
      "networks:\n" \
      "  default:\n" \
      "    external:\n" \
      "      name: network-rubix-build" \
      "services:\n" \
      "  rubix-master:\n" \
      "    build:\n" \
      "      context: .\n" \
      "      args:\n" \
      "        is_master: \"true\"\n" \
      "    volumes:\n" \
      "      - /tmp/rubix/tests:/tmp/rubix/tests\n" \
      "      - /tmp/rubix/jars:/usr/lib/rubix/lib\n" \
      "    networks:\n" \
      "      default:\n" \
      "        ipv4_address: 172.18.8.0\n"

wipv_address="172.18.8.1\n"
worker_str= ":  \n" \
            "    build:\n" \
            "      context: .\n" \
            "      args:\n" \
            "        is-master: \"false\"\n" \
            "    volumes:\n" \
            "      - /tmp/rubix/tests:/tmp/rubix/tests\n" \
            "      - /tmp/rubix/jars:/usr/lib/rubix/lib\n" \
            "    networks:\n" \
            "      default:\n" \
            "        ipv4_address: "


no_of_workers =  sys.argv[1]
path = sys.argv[2]
file = open(path+'/docker/docker-compose.yml',"w")
file.write(master_str)
for i in range(0, int(no_of_workers)):
    service_name = "rubix-worker-" + str(i + 1)
    ipv_address = "172.18.8." + str(i + 1)
    file.write(service_name + worker_str + ipv_address+"\n")
file.close()
