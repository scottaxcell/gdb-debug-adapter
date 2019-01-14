package com.coherentchaos.gdb.debugadapter.mi.output;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set name=value.
 */
// TODO rename and rewrite in my style
public class MIArg {
    String name;
    String value;

    public MIArg(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Parsing a DsfMIList of the form:
     * [{name="xxx",value="yyy"},{name="xxx",value="yyy"},..]
     * [name="xxx",name="xxx",..]
     * [{name="xxx"},{name="xxx"}]
     */
    public static MIArg[] getMIArgs(MIList miList) {
        List<MIArg> aList = new ArrayList<MIArg>();
        Value[] values = miList.getValues();
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Tuple) {
                MIArg arg = getMIArg((Tuple) values[i]);
                if (arg != null) {
                    aList.add(arg);
                }
            }
        }
        Result[] results = miList.getResults();
        for (int i = 0; i < results.length; i++) {
            Value value = results[i].getValue();
            if (value instanceof MIConst) {
                String str = ((MIConst) value).getcString();
                aList.add(new MIArg(str, "")); //$NON-NLS-1$
            }
        }
        return (aList.toArray(new MIArg[aList.size()]));
    }

    /**
     * Parsing a DsfMITuple of the form:
     * {{name="xxx",value="yyy"},{name="xxx",value="yyy"},..}
     * {name="xxx",name="xxx",..}
     * {{name="xxx"},{name="xxx"}}
     */
    public static MIArg[] getMIArgs(Tuple miTuple) {
        List<MIArg> aList = new ArrayList<MIArg>();
        Value[] values = miTuple.getValues();
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Tuple) {
                MIArg arg = getMIArg((Tuple) values[i]);
                if (arg != null) {
                    aList.add(arg);
                }
            }
        }
        Result[] results = miTuple.getResults();
        for (int i = 0; i < results.length; i++) {
            Value value = results[i].getValue();
            if (value instanceof MIConst) {
                String str = ((MIConst) value).getcString();
                aList.add(new MIArg(str, "")); //$NON-NLS-1$
            }
        }
        return (aList.toArray(new MIArg[aList.size()]));
    }

    /**
     * Parsing a DsfMITuple of the form:
     * {name="xxx",value="yyy"}
     * {name="xxx"}
     */
    public static MIArg getMIArg(Tuple tuple) {
        Result[] args = tuple.getResults();
        MIArg arg = null;
        if (args.length > 0) {
            // Name
            String aName = ""; //$NON-NLS-1$
            Value value = args[0].getValue();
            if (value != null && value instanceof MIConst) {
                aName = ((MIConst) value).getcString();
            }
            else {
                aName = ""; //$NON-NLS-1$
            }

            // Value
            String aValue = ""; //$NON-NLS-1$
            if (args.length == 2) {
                value = args[1].getValue();
                if (value != null && value instanceof MIConst) {
                    aValue = ((MIConst) value).getcString();
                }
                else {
                    aValue = ""; //$NON-NLS-1$
                }
            }

            arg = new MIArg(aName, aValue);
        }
        return arg;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + "=" + value; //$NON-NLS-1$
    }
}


