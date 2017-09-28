# convert downloaded obj file to the format that is ready to be copied into java class.
import sys


def main():
    filename = sys.argv[1]
    class_name = filename.split("/")[-1].split(".")[0]
    output_file_name = class_name + ".java"
    class_name = class_name.split("_")[0][:-1] + class_name.split("_")[1]
    class_name = class_name.title()
    with open(filename, 'r') as file:
        input_txt = file.read()
    lines = input_txt.split("\n")
    tex_coord_output = ""
    normal_output = ""
    face_output = ""
    output = """
    import android.util.Log;

import java.nio.Buffer;

public class """ + class_name + """ extends MeshObject{

    private Buffer mVertBuff;
    private Buffer mTexCoordBuff;
    private Buffer mNormBuff;
    private Buffer mIndBuff;

    private int indicesNumber = 0;
    private int verticesNumber = 0;


    public """ + class_name + """() {
        setVerts();
        setTexCoords();
        setNorms();
        setIndices();
        Log.d("DDL", "vert length:" + Integer.toString(verticesNumber));

        Log.d("DDL", "ind length:" + Integer.toString(indicesNumber));

    }

    private void setVerts()
    {
        double[] HAND_VERTS = {
    """
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
    output = output + """
            };
        mVertBuff = fillBuffer(HAND_VERTS);
        verticesNumber = HAND_VERTS.length / 3;
    }

    private void setTexCoords() {
        double[] HAND_TEX_COORDS = {"""
    for v in vn_lst:
        v1 = v.split(" ")[1]
        v2 = v.split(" ")[2]
        v3 = v.split(" ")[3]
        normal_output = normal_output + v1 + ", " + v2 + ", " + v3.strip() + ", \n"

    for v in f_lst:
        v1 = v.split(" ")[1].split("/")[0]
        v2 = v.split(" ")[2].split("/")[0]
        v3 = v.split(" ")[3].split("/")[0]
        face_output = face_output + str(int(v1)-1) + ", " + str(int(v2)-1) + ", " + str(int(v3.strip())-1) + ", \n"
    for i in range(len(f_lst)/3):
        tex_coord_output = tex_coord_output + "0.0, 0.0, 0.0, 0.1, 0.1, 0.0,\n"

    output = output + tex_coord_output
    output = output + "\n\n"
    output = output + """        };
        Log.d("DDL", "tex length:" + Integer.toString(HAND_TEX_COORDS.length));

        mTexCoordBuff = fillBuffer(HAND_TEX_COORDS);
    }

    private void setNorms()
    {
       double[] HAND_NORMS = {"""

    output = output + normal_output
    output = output + "\n\n"
    output = output + """      };
        Log.d("DDL", "norm length:" + Integer.toString(HAND_NORMS.length));

        mNormBuff = fillBuffer(HAND_NORMS);
    }

    private void setIndices() {
        short[] HAND_INDICES = {"""

    output = output + face_output

    output = output + """       };
        mIndBuff = fillBuffer(HAND_INDICES);
        indicesNumber = HAND_INDICES.length;
    }

    public int getNumObjectIndex() {
        return indicesNumber;

    }

    @Override
    public int getNumObjectVertex() {
        return verticesNumber;
    }

    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType) {
        Buffer result = null;
        switch (bufferType) {
            case BUFFER_TYPE_VERTEX:
                result = mVertBuff;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = mTexCoordBuff;
                break;
            case BUFFER_TYPE_NORMALS:
                result = mNormBuff;
                break;
            case BUFFER_TYPE_INDICES:
                result = mIndBuff;
            default:
                break;
        }
        return result;
    }
}
"""

    with open(output_file_name, 'w') as output_file:
        output_file.write(output)


if __name__ == "__main__":
    main()
