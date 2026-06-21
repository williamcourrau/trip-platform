# Load the restart_process extension
load('ext://restart_process', 'docker_build_with_restart')

### K8s Config ###
k8s_yaml('./infra/development/k8s/namespace.yaml')
k8s_yaml('./infra/development/k8s/app-config.yaml')

### End of K8s Config ###

### API Gateway ###

gateway_compile_cmd = 'mvn package -pl services/api-gateway -am -DskipTests -q'
if os.name == 'nt':
  gateway_compile_cmd = 'mvn package -pl services/api-gateway -am -DskipTests -q'

local_resource(
  'api-gateway-compile',
  gateway_compile_cmd,
  deps=['./services/api-gateway', './services/common'], labels="compiles")

docker_build_with_restart(
  'trip-platform/api-gateway',
  '.',
  entrypoint=['java', '-jar', '/app/services/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar'],
  dockerfile='./infra/development/docker/api-gateway.Dockerfile',
  only=[
    './services/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar',
    './services/common/target/common-0.0.1-SNAPSHOT.jar',
  ],
  live_update=[
    sync('./services/api-gateway/target', '/app/services/api-gateway/target'),
  ],
)

k8s_yaml('./infra/development/k8s/api-gateway-deployment.yaml')
k8s_resource('api-gateway', port_forwards=8080,
             resource_deps=['api-gateway-compile'], labels="services")
### End of API Gateway ###

### Trip Service ###

trip_compile_cmd = 'mvn package -pl services/trip-service -am -DskipTests -q'
if os.name == 'nt':
  trip_compile_cmd = 'mvn package -pl services/trip-service -am -DskipTests -q'

local_resource(
  'trip-service-compile',
  trip_compile_cmd,
  deps=['./services/trip-service', './services/common'], labels="compiles")

docker_build_with_restart(
  'trip-platform/trip-service',
  '.',
  entrypoint=['java', '-jar', '/app/services/trip-service/target/trip-service-0.0.1-SNAPSHOT.jar'],
  dockerfile='./infra/development/docker/trip-service.Dockerfile',
  only=[
    './services/trip-service/target/trip-service-0.0.1-SNAPSHOT.jar',
    './services/common/target/common-0.0.1-SNAPSHOT.jar',
  ],
  live_update=[
    sync('./services/trip-service/target', '/app/services/trip-service/target'),
  ],
)

k8s_yaml('./infra/development/k8s/trip-service-deployment.yaml')
k8s_resource('trip-service', port_forwards=8081,
             resource_deps=['trip-service-compile'], labels="services")
### End of Trip Service ###

### Driver Service ###

driver_compile_cmd = 'mvn package -pl services/driver-service -am -DskipTests -q'
if os.name == 'nt':
  driver_compile_cmd = 'mvn package -pl services/driver-service -am -DskipTests -q'

local_resource(
  'driver-service-compile',
  driver_compile_cmd,
  deps=['./services/driver-service', './services/common'], labels="compiles")

docker_build_with_restart(
  'trip-platform/driver-service',
  '.',
  entrypoint=['java', '-jar', '/app/services/driver-service/target/driver-service-0.0.1-SNAPSHOT.jar'],
  dockerfile='./infra/development/docker/driver-service.Dockerfile',
  only=[
    './services/driver-service/target/driver-service-0.0.1-SNAPSHOT.jar',
    './services/common/target/common-0.0.1-SNAPSHOT.jar',
  ],
  live_update=[
    sync('./services/driver-service/target', '/app/services/driver-service/target'),
  ],
)

k8s_yaml('./infra/development/k8s/driver-service-deployment.yaml')
k8s_resource('driver-service', port_forwards=8082,
             resource_deps=['driver-service-compile'], labels="services")
### End of Driver Service ###

### Payment Service ###

payment_compile_cmd = 'mvn package -pl services/payment-service -am -DskipTests -q'
if os.name == 'nt':
  payment_compile_cmd = 'mvn package -pl services/payment-service -am -DskipTests -q'

local_resource(
  'payment-service-compile',
  payment_compile_cmd,
  deps=['./services/payment-service', './services/common'], labels="compiles")

docker_build_with_restart(
  'trip-platform/payment-service',
  '.',
  entrypoint=['java', '-jar', '/app/services/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar'],
  dockerfile='./infra/development/docker/payment-service.Dockerfile',
  only=[
    './services/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar',
    './services/common/target/common-0.0.1-SNAPSHOT.jar',
  ],
  live_update=[
    sync('./services/payment-service/target', '/app/services/payment-service/target'),
  ],
)

k8s_yaml('./infra/development/k8s/payment-service-deployment.yaml')
k8s_resource('payment-service', port_forwards=8083,
             resource_deps=['payment-service-compile'], labels="services")
### End of Payment Service ###

### Web Frontend (TypeScript) ###

docker_build(
  'trip-platform/web',
  '.',
  dockerfile='./infra/development/docker/web.Dockerfile',
)

k8s_yaml('./infra/development/k8s/web-deployment.yaml')
k8s_resource('web', port_forwards=3000, labels="frontend")

### End of Web Frontend ###
