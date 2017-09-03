# scale object file
import sys

def main():
    filename = sys.argv[1]
    with open(filename, 'r') as file:
        input_txt = file.read()
    lines = input_txt.split("\n")
    output = ""
    v_lst = []
    others_lst = []
    output = ""
    for line in lines:
        if len(line) > 0:
            tokens = line.split(" ")
            if tokens[0] == "v":
                v1 = float(tokens[1]) / 100.0
                v2 = float(tokens[2]) / 100.0
                v3 = float(tokens[3]) / 100.0
                output = output + "v " + str(v1) + " " + str(v2) + " " + str(v3) + " " + tokens[4] + " " + tokens[5] + " " + tokens[6] + "\n";
            else:
                output = output + line + "\n"
    with open("scaled_" + filename, 'w') as output_file:
        output_file.write(output)


if __name__ == "__main__":
    main()
