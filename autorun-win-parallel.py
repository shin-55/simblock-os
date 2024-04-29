# -*- coding: utf-8 -*-
import datetime
import itertools
import os
import subprocess

import openpyxl  # noqa : must import required pandas
import pandas as pd
import uuid
import hashlib
import os
import shutil
import zipfile
import multiprocessing.pool
from concurrent.futures import ThreadPoolExecutor

now = datetime.datetime.now()
datestr = f"{now:%Y%m%d}"

simconf_path = "simulator/src/main/java/simblock/settings/ProposalConfiguration.java"
# simconf_path = "tempconf.java"

LOOP_COUNT = 100
MAX_THREAD_COUNT = 10
nodes = [500]
internal_nodes = [7, 6, 5, 4]
n_roots = [2, 3, 4]
is_proposals = [True, False]

org_template = """
package simblock.settings;

public class ProposalConfiguration {
    public static final int NUM_OF_NODES = {node};
    public static final int MAX_OUTBOUND_NUM = 8;
    public static final int MAX_INBOUND_NUM = 8;
    public static final int INTERNAL_FORWARD_NUM = {internal_node};
    public static final double N_ROOT = {n_root};  // n乗根のnの数
    public static final int OUTBOUND_FOREGIN_NUM = MAX_OUTBOUND_NUM - INTERNAL_FORWARD_NUM;
    public static final int INBOUND_FOREGIN_NUM = MAX_INBOUND_NUM - INTERNAL_FORWARD_NUM;
    public static final int FOREGION_REGION_NUM = OUTBOUND_FOREGIN_NUM; 
    public static final boolean IS_PROPOSAL_USE = {is_proposal};
}
"""


def write_config(template):
    with open(simconf_path, "w", encoding="utf-8") as fp:
        fp.write(template)
    print("write success")

def task(params):
    cmd = params[0]
    loop_result_dir = params[1]
    # copy dist
    shutil.copyfile("simulator/build/distributions/simulator.zip", loop_result_dir + "/simulator.zip")
    #unzip simulator.zip
    with zipfile.ZipFile(loop_result_dir + "/simulator.zip") as existing_zip:
        existing_zip.extractall(loop_result_dir)

    os.system(cmd)
    with open(f"{loop_result_dir}/result.txt", encoding="utf-8") as fp:
        result = fp.read().split("\n")
    fp.close()
    result = [line for line in result if line.startswith(";")]
    result = [line.split(";")[1] for line in result]
    # save to result 2
    with open(f"{loop_result_dir}/result2.txt", "w", encoding="utf-8") as fp2:
        fp2.write("\n".join(result))
    fp2.close()
    os.system(f"python simtime_parse-parallel.py \"{loop_result_dir}/simulator/output/output.json\" | find \"block_id\" > {loop_result_dir}/result3.txt")
    return "Task finished: " + loop_result_dir

def launch_simulator(param_str):
    date = datetime.datetime.today().strftime('%Y%m%d')
    time = datetime.datetime.today().strftime('%H%M%S')
    uuid_hash = hashlib.sha256(str(uuid.uuid4()).encode('UTF-8')).hexdigest()
    uuid_short = uuid_hash[:10]

    result_dir = f"result/{date}/{time}_{uuid_short}/{param_str}"
    if not os.path.exists(result_dir):
        os.makedirs(result_dir)
    
    # gradle build
    os.system(f"gradle build")
    work_dirs = []
    for i in range(1, LOOP_COUNT+1):
        loop_result_dir = f"result/{date}/{time}_{uuid_short}/{param_str}/{i}"
        if not os.path.exists(loop_result_dir):
            os.makedirs(loop_result_dir)
        work_dirs.append([f"{loop_result_dir}/simulator/bin/runSimBlock.bat > {loop_result_dir}/result.txt"
                                .replace("/", "\\"), 
                                loop_result_dir,])
    
    with ThreadPoolExecutor(MAX_THREAD_COUNT) as executor:
        results = executor.map(task, work_dirs)
        for result in results:
            print(result)

    os.system(f"python calc_simuration_time.py --root_directory \"{result_dir}\" > {result_dir}/summary.txt")

    print(uuid_short)
    return uuid_short


def replace_template(org_template, node, internal_node, is_proposal, n_root):
    template = org_template.replace("{node}", str(node))
    template = template.replace("{internal_node}", str(internal_node))
    template = template.replace(
        "{is_proposal}", "true" if is_proposal else "false")
    template = template.replace("{n_root}", str(n_root))
    return template


# 特定の文字列を含むディレクトリを取得する関数
def get_directories_containing_string(root_directory, search_string):
    matching_directories = []
    for root, directories, files in os.walk(root_directory):
        for directory in directories:
            if search_string in directory:
                matching_directories.append(os.path.join(root_directory, directory))
                break
        else:
            for directory in directories:
                nextroot_dir = os.path.join(root_directory, directory)
                matching_directories = get_directories_containing_string(nextroot_dir, search_string)
                if len(matching_directories) > 0:
                    break
        if len(matching_directories) > 0:
            break
    return matching_directories


def get_result(root_directory, prefix):
    # 特定の文字列を含むディレクトリを取得
    matching_dirs = get_directories_containing_string(
        root_directory, prefix)
    target_dir = matching_dirs[0]

    target_summary = f"{target_dir}/summary.txt"
    with open(target_summary, encoding="utf-8") as fp:
        result = fp.read().split("\n")

    sec_result = result[-2]
    print(sec_result)
    # 空白除去
    input_string = sec_result.replace(" ", "")
    # 文字列をカンマと空白で分割
    split_values = input_string.split(",")

    # 空の辞書を作成し、各要素を追加
    output_dict = {}
    for val in split_values:
        key, value = val.split(":")
        output_dict[key] = float(value) if '.' in value else int(value)

    print(output_dict)
    return output_dict


def save_result_summary(df, filename: str, sheet_name: str):
    if os.path.exists(filename):
        with pd.ExcelWriter(filename, mode="a", engine="openpyxl", if_sheet_exists="replace") as writer:
            df.to_excel(writer, sheet_name=sheet_name,
                        index=False, header=False)
    else:
        with pd.ExcelWriter(filename, engine="openpyxl") as writer:
            df.to_excel(writer, sheet_name=sheet_name,
                        index=False, header=False)


result = [
    ["手法", "IN(地域外:地域内)", "OUT(地域外:地域内)", "ノード数",
     "ブロック長", "最小値", "最大値", "平均値", "中央値"]
]


for internal_node, is_proposal, node in itertools.product(internal_nodes, is_proposals, nodes): # internal_node, is_proposal, nodeを組み合わせて集合を作成
    if is_proposal:
        for n_root in n_roots:
            # 提案
            template = replace_template(
                org_template, node, internal_node, is_proposal, n_root)
            write_config(template)
            prefix = f"n{node}_vs{internal_node}_{n_root}root"
            process_uuid = launch_simulator(prefix)
            root_directores = get_directories_containing_string("result", process_uuid)
            ret = get_result(root_directores[0], prefix)
            result.append([f"提案({n_root}√)", f"vs{internal_node}", f"vs{internal_node}",
                          node, 200, ret["min"], ret["max"], ret["mean"], ret["median"]])
    else:
        # 先行研究
        template = replace_template(
            org_template, node, internal_node, is_proposal, 1)
        write_config(template)
        prefix = f"n{node}_vs{internal_node}_related"
        process_uuid = launch_simulator(prefix)
        root_directores = get_directories_containing_string("result", process_uuid)
        ret = get_result(root_directores[0], prefix)
        result.append([f"既存", f"vs{internal_node}", f"vs{internal_node}",
                      node, 200, ret["min"], ret["max"], ret["mean"], ret["median"]])

dataframe = pd.DataFrame(result)
save_result_summary(
    dataframe, f"result/{datestr}/result_summary.xlsx", f"{now:%H%M%S}")
