# convert quad to triangle
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
        output = output + v + "\n"
    for vn in vn_lst:
        output = output + vn + "\n"
    for f in f_lst:
        v1 = f.split(" ")[1].split("//")[0]
        v2 = f.split(" ")[2].split("//")[0]
        v3 = f.split(" ")[3].split("//")[0]
        v4 = f.split(" ")[4].split("//")[0]
        output = output + "f " + v1 + " " + v2 + " " + v3 + "\n"
        output = output + "f " + v4 + " " + v3 + " " + v1 + "\n"
    with open("tri.obj", 'w') as output_file:
        output_file.write(output)



if __name__ == "__main__":
    main()
