import os
# import pandas as pd
# import openpyxl # import required pandas


def find_and_read_file(root_dir, target_filename):
    for dirpath, dirnames, filenames in os.walk(root_dir):
        if target_filename in filenames:
            with open(os.path.join(dirpath, target_filename), 'r') as f:
                data = f.read()
                yield data

def save_result_summary(df, filename: str, sheet_name: str):
    if os.path.exists(filename):
        with pd.ExcelWriter(filename, mode="a", engine="openpyxl", if_sheet_exists="replace") as writer:
            df.to_excel(writer, sheet_name=sheet_name, index=False, header=False)
    else:
        with pd.ExcelWriter(filename, engine="openpyxl") as writer:
            df.to_excel(writer, sheet_name=sheet_name, index=False, header=False)


if __name__ == '__main__':
    import argparse
    import json
    import numpy as np

    parser = argparse.ArgumentParser()
    parser.add_argument("--root_directory", type=str, required=True)
    parser.add_argument("--filename_to_find", type=str, default="result3.txt")
    args = parser.parse_args()

    all_penetrations = set()
    for file_content in find_and_read_file(args.root_directory, args.filename_to_find):
        if file_content:
            print(f"File content:\n{file_content}")
        else:
            print(f"No file named '{args.filename_to_find}' was found in the directory tree rooted at '{args.root_directory}'.")
            continue
        lines = file_content.split('\n')
        for line in lines:
            data_parts = line.split(", ")
            if len(data_parts) != 4:
                continue
            penetration_str = data_parts[3]
            penetration_value = int(penetration_str.split(": ")[1])
            all_penetrations.add(penetration_value)
    # print(json.dumps(list(all_penetrations)))
    all_penetrations = np.array(list(all_penetrations))
    print(f"min: {np.min(all_penetrations)}, max: {np.max(all_penetrations)}, mean: {np.mean(all_penetrations)}, median: {np.median(all_penetrations)}")

    # summary = [
    #     [""]
    # ]
    # df_summary = pd.DataFrame(summary)
    # save_result_summary(df_summary, 'result.xlsx', 'result')
