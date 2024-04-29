#!/bin/bash

EXEC_DATE=$(date +"%Y%m%d")
EXEC_TIME=$(date +"%H%M%S")
# UUIDをSHA256ハッシュに変換し、その後10文字を取得
UUID_HASH=$(echo -n $(uuidgen) | sha256sum)
UUID_HASH_SHORT=$(echo $UUID_HASH | cut -c 1-10)
# 保存先
ORG_RESULT_DIR="result/${EXEC_DATE}/${EXEC_TIME}_${UUID_HASH_SHORT}/${1}"
echo ${UUID_HASH_SHORT}
# RESULT_DIR="/vmimage/${EXEC_DATE}"
if ! ls ${ORG_RESULT_DIR}; then
    mkdir -p ${ORG_RESULT_DIR}
fi

function main(){
    ORG_RESULT_DIR="$1"
    LOOPCOUNT="$2"

    LOOP_RESULT_DIR="${ORG_RESULT_DIR}/${LOOPCOUNT}"
    if ! ls ${LOOP_RESULT_DIR}; then
        mkdir -p ${LOOP_RESULT_DIR}
    fi
    gradle build && gradle :simulator:run > ${LOOP_RESULT_DIR}/result.txt
    cat ${LOOP_RESULT_DIR}/result.txt | grep "^;" | cut -d ";" -f2 > ${LOOP_RESULT_DIR}/result2.txt
    python simtime_parse.py | grep block_id > ${LOOP_RESULT_DIR}/result3.txt
}

for i in {1..100}
do
    main "$ORG_RESULT_DIR" "$i"
done
# 1回だけ
# main "$ORG_RESULT_DIR" "$i"


python calc_simuration_time.py --root_directory "${ORG_RESULT_DIR}" > ${ORG_RESULT_DIR}/summary.txt