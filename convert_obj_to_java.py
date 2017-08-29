# convert downloaded obj file to the format that is ready to be copied into java class.
import sys


def main():
    filename = sys.argv[1]
    with open(filename, 'r') as file:
        input_txt = file.read()
    lines = input_txt.split("\n")
    output = ""
    v_lst = []
    vn_lst = []
    f_lst = []
    for line in lines:
        if len(line) > 0:
            tokens = line.split(" ")
            if tokens[0] == "v":
                v_lst.append(line)
            elif tokens[0] == "vn":
                vn_lst.append(line)
            elif tokens[0] == "f":
                f_lst.append(line)
    for v in v_lst:
        v1 = v.split(" ")[1]
        v2 = v.split(" ")[2]
        v3 = v.split(" ")[3]
        output = output + v1 + ", " + v2 + ", " + v3.strip() + ", \n"
    output = output + "\n\n"
    for v in vn_lst:
        v1 = v.split(" ")[1]
        v2 = v.split(" ")[2]
        v3 = v.split(" ")[3]
        output = output + v1 + ", " + v2 + ", " + v3.strip() + ", \n"
    output = output + "\n\n"
    for v in f_lst:
        v1 = v.split(" ")[1].split("//")[0]
        v2 = v.split(" ")[2].split("//")[0]
        v3 = v.split(" ")[3].split("//")[0]
        output = output + str(int(v1)-1) + ", " + str(int(v2)-1) + ", " + str(int(v3.strip())-1) + ", \n"
    output = output + "\n\n"
    for i in range(len(f_lst)/3):
        output = output + "0.0, 0.0, 0.0, 0.1, 0.1, 0.0,\n"
    with open("java_data.txt", 'w') as output_file:
        output_file.write(output)


if __name__ == "__main__":
    main()
