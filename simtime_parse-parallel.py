import json
from pprint import pprint
import sys

# output.jsonの読み込み
# filepath = "simulator/src/dist/output/output.json"
filepath = sys.argv[1]
with open(filepath) as fp:
    jsondata = json.load(fp)

# jsonは配列なので、配列内の1要素ごとに処理していく
block_dict = {}
for row in jsondata:
    # contentキーが含まれない場合はその行は無視する
    content = row.get("content")
    if content is None:
        continue
    # block-idキーがcontentに含まれない場合はその行は無視する
    block_id = content.get("block-id")
    if block_id is None:
        continue

    # block_idをキーにした配列がblock_dictに存在していた場合は、
    # その配列を取得して、そうでない場合は配列を初期化する
    block_id_list = block_dict.get(block_id, [])

    # transmission-timestampが取得できる場合はその値をblock_id_listに追加する
    timestamp = content.get("transmission-timestamp")
    if timestamp is not None:
        block_id_list.append(timestamp)
    # timestampが取得できる場合はその値をblock_id_listに追加する
    timestamp = content.get("timestamp")
    if timestamp is not None:
        block_id_list.append(timestamp)

    # 最後にblock_idをキーにしてリストを上書きする
    block_dict[block_id] = block_id_list


# 結果表示
result = []
for block_id, timestamps in block_dict.items():
    # sorted_block_id_list = sorted(block_id_list, key=lambda x: x["timestamp"])
    # pprint(timestamps)
    # リストの最小値を取得
    min_timestamp = min(timestamps)
    # リストの最大値を取得
    max_timestamp = max(timestamps)
    # 浸透に要した時間
    all_time = max_timestamp - min_timestamp
    # 結果の画面表示
    print(f"block_id: {block_id}, min_timestam: {min_timestamp}, max_timestamp: {max_timestamp}, penetration: {all_time}")
