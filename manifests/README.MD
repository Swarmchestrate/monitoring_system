# EMS Deployment Manual

## 0. Get Superuser Rights

Run the following command to obtain superuser privileges (or use `sudo` before each command):

```bash
sudo -i
```

---

# Deployment Steps

## 1. Update TOSCA Configuration

For the **TOSCA model** that RA has received:

- An **ID is created**
- The **TOSCA model is persisted in the OptimusDB**

Update the following keys in the configuration file (myconfig.yaml).

**File location:**

```
/opt/iccs/monitoring_system/manifests/manifests_for_demo/myconfig.yaml
```

### Keys to Update

**`TOSCA_MODEL_FILE`**
The TOSCA model file that includes the monitoring instractions EMS needs for its deployment. As discussed this will be an ID.
Note: This will be changed to **`SWARM_ID`**

**`TOSCA_MODEL_PATH`**

- This will **not be needed to change, after the new release**

---

## 2. Apply the Environment Variables

Apply the environment variables using the following command:

```bash
kubectl apply -f /opt/iccs/monitoring_system/manifests/manifests_for_demo/myconfig.yaml
```

---

## 3. Deploy EPM

Deploy **EPM** using:

```bash
kubectl apply -f /opt/iccs/monitoring_system/manifests/manifests_for_demo/ems+netdata-k3s_parametric.yaml
```

**Note:**
EPM deploys the **EPAs in all available nodes**.

---

## 4. Deploy the Demo Application

Deploy the Demo app using:

```bash
kubectl apply -f /opt/iccs/monitoring_system/manifests/app-k3s.yaml
```

---

## 5. Deploy STOMP Listener

Deploy the event listener:

```bash
kubectl apply -f /opt/iccs/monitoring_system/manifests/stomp-listener.yaml
```

---

# STOMP Listener

Details about drafting the STOMP listener script (which is available at the following repository):

https://github.com/Swarmchestrate/monitoring_system/blob/main/stomp_listener/listener.py

The script is used to **create the container image that is deployed**. It can either:

- Be integrated into other code, or
- Be used directly to build a deployment image.

---

# Broker Configuration

Example configuration in the listener script:

```python
# Broker details

host = 'emsserver-ems-server'  # the service name or your broker IP
port = 61610                   # default STOMP port
username = ''                  # insert broker credentials
password = ''                  # insert broker credentials
destination = '/topic/>'       # or /topic/your-topic for pub-sub
```

## Parameter Description

**host**

- If running in the same cluster, set:

```
host = "service name of the EMS server"
```

**port**

- Set when the **STOMP protocol** is used.

**username**

- Broker credentials username.

**password**

- Broker credentials password.

**destination**

Specifies the topic the listener should subscribe to.

Examples:

```
/topic/cpu_util
```

Subscribe to a **specific topic**.

```
/topic/>
```

Subscribe to **all available topics**.
