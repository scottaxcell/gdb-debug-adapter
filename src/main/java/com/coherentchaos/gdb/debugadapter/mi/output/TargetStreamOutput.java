package com.coherentchaos.gdb.debugadapter.mi.output;

import com.coherentchaos.gdb.debugadapter.mi.record.StreamRecord;

/**
 * "@" c-string nl
 * The target output stream contains textual output from the running target. Only present for remote targets.
 */
public class TargetStreamOutput extends StreamRecord {
}
