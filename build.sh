#!/usr/bin/env bash

set -eo pipefail

cur_dir=$(cd $(dirname $0); pwd)
cur_user="$(id -u):$(id -g)"

BUILDING_JDK_IMAGE=codingcorp-docker.pkg.coding.net/registry/release/ubuntu:16.04-konajdk8
BASE_IMAGE=codingcorp-docker.pkg.coding.net/registry/release/ubuntu:16.04-konajdk8

REGISTRY_URL="${REGISTRY_URL:-docker-registry.coding.net}"
REGISTRY_USER="${REGISTRY_USER:-testing}"
REGISTRY_PASS="${REGISTRY_PASS:-McBdGVqvtJ9mRpeQ}"

CODING_REGISTRY_URL="${CODING_REGISTRY_URL:-codingcorp-docker.pkg.coding.net}"
CODING_REGISTRY_REPO="${CODING_REGISTRY_REPO:-platform/platform-release}"
CODING_REGISTRY_USER="${CODING_REGISTRY_USER:-dt_uQFZxOtEdV}"
CODING_REGISTRY_PASS="${CODING_REGISTRY_PASS:-nDZT7n47irtOEkOSj5ps2hhocFwcaP}"

IMAGE_BUILD_PREFIX="${IMAGE_BUILD_PREFIX:-codingcorp-docker.pkg.coding.net/registry/build}"
IMAGE_RELEASE_PREFIX="${IMAGE_RELEASE_PREFIX:-codingcorp-docker.pkg.coding.net/registry/release}"

function status() {
    echo -e "\033[35m >>>   $*\033[0;39m"
}

# Fix docker signal proxy issue without tty (moby/moby#2838)
function docker() {
    case "$1" in
        run)
            shift
            if [ -t 1 ]; then # have tty
                command docker run --init -it "$@"
            else
                id=`command docker run -d --init "$@"`
                trap "command docker kill ${id}" INT TERM
                command docker logs -f ${id} &
                return $(command docker wait ${id})
            fi
            ;;
        *)
            command docker "$@"
    esac
}

# export to sub-shell
export -f docker

function build_java() {
    module=$1
    echo "module: ${module}"
    resource_dir="${cur_dir}/$(echo ${module} | sed 's#:#/#g')/src/main/resources"
    echo "resource_dir: ${resource_dir}"
    if [ -d "${resource_dir}" ] \
        && [ -f "${resource_dir}/application.properties.example" ]; then
        cp "${resource_dir}/application.properties.example" "${resource_dir}/application.properties"
    fi

    if [ `uname` == "Darwin" ]; then
        ${cur_dir}/gradlew -p ${cur_dir} ${module}:buildDockerImage
    else
        docker run --rm \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -v ${cur_dir}:/app \
            -w /app \
            -e GRADLE_USER_HOME=/app/.gradle \
            -e BASE_IMAGE=$BASE_IMAGE \
            -e NO_ERRORPRONE=true \
            $BUILDING_JDK_IMAGE \
            bash -cx "./gradlew -Dorg.gradle.daemon=false ${module}:buildDockerImage"
    fi
}

function push_image() {
    local module="platform-project"
    local version=`git rev-parse HEAD`
    local image="${module}:${version}"

    if [ -z "$CODING_REGISTRY_USER" ]
    then
        echo "[WARN] CODING_REGISTRY_USER is empty, skipped push to CODING_REGISTRY_URL"
    else
        echo "[INFO] pushing image to ${CODING_REGISTRY_URL}"
        if [ $USING_KONA_JDK == "true" ]; then
            local coding_registry_image="${CODING_REGISTRY_URL}/${CODING_REGISTRY_REPO}/${module}-tjdk:${version}"
        else
            local coding_registry_image="${CODING_REGISTRY_URL}/${CODING_REGISTRY_REPO}/${image}"
        fi
        docker tag ${image} ${coding_registry_image}
        docker push ${coding_registry_image}
        docker rmi ${coding_registry_image} || true
    fi

    docker rmi ${image} || true
    tagging_image
}

function tagging_image() {
    local app_name="platform-project"
    local version=`git rev-parse HEAD`
    local datetime=`date +"%Y%m%d"`

    if [[ -n "${TO}" ]]; then
        echo "begin tagging date image..."

        local registry_from="${IMAGE_BUILD_PREFIX}/${app_name}:${version}"
        local registry_to="${IMAGE_RELEASE_PREFIX}/${app_name}:${datetime}.${TO}"

        echo "tagging ${registry_from} to ${registry_to}"
        docker pull ${registry_from}
        echo "pull ${registry_from}"
        docker tag ${registry_from} ${registry_to}
        echo "tag ${registry_from} to ${registry_to}"
        docker push ${registry_to}
        echo "push ${registry_to}"

        docker rmi ${registry_from} ${registry_to} || true
    fi

    if [[  -n "${MASTER}" ]]; then
        echo "begin tagging master image..."

        local registry_from="${IMAGE_BUILD_PREFIX}/${app_name}:${version}"
        local registry_master="${IMAGE_RELEASE_PREFIX}/${app_name}:master"

        echo "tagging ${registry_from} to ${registry_master}"
        docker pull ${registry_from}
        docker tag ${registry_from} ${registry_master}
        docker push ${registry_master}

        docker rmi ${registry_from} ${registry_master} || true
    fi
}

function project_name() {
    sed "s#\"#'#g" settings.gradle |  grep -oP "rootProject.+'\K([^']+)"
}

if ! grep -q ${CODING_REGISTRY_URL} ~/.docker/config.json; then
    status "logging into ${CODING_REGISTRY_URL}..."
    echo ${CODING_REGISTRY_PASS} | docker login -u ${CODING_REGISTRY_USER} --password-stdin ${CODING_REGISTRY_URL} || echo "[ERROR] Login $CODING_REGISTRY_URL failed"
fi

build_java app

if [ "$1" = "--push" ]; then
    status "pushing docker image to ${REGISTRY_URL}..."
    push_image
fi
