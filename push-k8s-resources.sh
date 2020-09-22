#!/bin/bash

# 把项目中的 deploy 目录执行到

# k8s 资源加载
# 需要设置环境变量
# AUTO_DEPLOY_USER  必选项  项目令牌 username  必选项  git 拉取代码的时候使用
# AUTO_DEPLOY_PASS  必选项  项目令牌 password  必选项  git 拉取代码的时候使用
# VERSION           必选项  把内容打包到制品库时，需要指定日期
# SERVICE_NAME      可选项  用于提交到 auto-deploy 项目的文件夹名称

username=${AUTO_DEPLOY_USER}
password=${AUTO_DEPLOY_PASS}
version=${VERSION_TAG}
servicename=${SERVICE_NAME}

if [ "$version" = "" ];then
  echo "not set VERSION env variable, so not push auto-deploy project."
  exit 0;
fi

if [ "$username" = "" ] || [ "$password" = "" ] || [ "$version" = "" ]; then
  echo "Usage: ./config.sh"
  echo "must be set env variable."
  echo " AUTO_DEPLOY_USER: to pull auto-deploy project by git "
  echo " AUTO_DEPLOY_PASS: to pull auto-deploy project by git "
  echo " VERSION: to push generic mark version"
  echo "Optional set SERVICE_NAME env variable to define dir in auto-deploy project"
  exit 1;
fi


is_empty_dir() {
  return $(ls -A $1 | wc -w) # 判断是否存在目录 和 文件
}

function parse_yaml() {
  local prefix=$2
  local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @ | tr @ '\034')
  sed -ne "s|^\($s\):|\1|" \
    -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
    -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p" $1 |
    awk -F$fs '{
      indent = length($1)/2;
      vname[indent] = $2;
      for (i in vname) {if (i > indent) {delete vname[i]}}
      if (length($3) > 0) {
         vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
         printf("%s%s%s=\%s\n", "'$prefix'",vn, $2, $3);
      }
   }'
}

# 获取服务名 $1=deployment.yaml | statefulset1.yaml
function parse_servicename() {
  parse_yaml "deploy/$1" | grep metadata_name | sed s/metadata_name=//g
}

# 是否存在 deploy 目录
is_empty_dir "deploy" >/dev/null 2>&1
if [ $? = 0 ]; then
  echo "not files in deploy dir"
  exit 1
fi

# 参数未传入 servicename
if [ "$servicename" = "" ]; then
  # 获取 k8s 资源文件
  filename=""
  if [ -f "deploy/deployment.yaml" ]; then
    filename="deploy/deployment.yaml"
  else
    if [ -f "deploy/statefulset.yaml" ]; then
      filename="deploy/statefulset.yaml"
    fi
  fi

  # 没有定义一个标准的名称，那就遍历内容， 根据 kind 查找
  if [ "$filename" = "" ]; then
    files=$(ls -A "deploy" | grep .yaml)
    for f in $files; do
      temp=$(parse_yaml "deploy/$f" | grep 'kind=Deployment\|kind=StatefulSet')
      if [ "$temp" != "" ]; then
        filename="deploy/$f"
      fi
    done
  fi

  if [ "$filename" = "" ];then
    echo "not found k8s resources file."
    echo "Usage: "
    echo "define env variable: "
    echo "ex: use deployment.yaml to define Deployment"
    echo "ex: use statefulset.yaml to define StatefulSet"
    exit 1
  fi

  servicename=$(parse_yaml "$filename" | grep metadata_name | sed s/metadata_name=//g)
fi

if [ "$servicename" = "" ];then
  echo "not found servicename to define dir in auto-deploy project"
  exit 1;
fi

echo "$servicename"


# 是否存在 auto-deploy 目录
is_empty_dir "auto-deploy" >/dev/null 2>&1
if [ $? != 0 ]; then
  echo "auto-deploy dir already exist, remove it and git clone new one"
  rm -fr auto-deploy
fi

git clone "https://$username:$password@e.coding.net/codingcorp/platform/auto-deploy.git"


if [ ! -d "auto-deploy/services/$servicename" ];then
  echo "create $servicename dir in services dir"
  mkdir "auto-deploy/services/$servicename"
fi

os=$(uname -s)
if [ "$os" = "Linux" ]; then
  cp -r deploy/* "auto-deploy/services/$servicename"
else
  cp -r deploy/ "auto-deploy/services/$servicename"
fi


cd auto-deploy

git add .
git commit -m "$servicename $version"
git push origin

cd -

tarname="$servicename.$version.tar.gz"
tar -czvf "$tarname" deploy
curl -T "$tarname" -u "$AUTO_DEPLOY_USER:$AUTO_DEPLOY_PASS" "https://codingcorp-generic.pkg.coding.net/platform/auto-config/$servicename?version=$version"


exit 0;