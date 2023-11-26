package http.libs;

public class HandleRetu {

    private boolean flag;
    private String desc;

    public HandleRetu(boolean flag, String desc){
        this.flag = flag;
        this.desc = desc;
    }

    public boolean getFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }


}
