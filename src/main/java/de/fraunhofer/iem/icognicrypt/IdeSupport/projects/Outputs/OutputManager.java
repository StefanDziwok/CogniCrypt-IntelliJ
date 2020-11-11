package de.fraunhofer.iem.icognicrypt.IdeSupport.projects.Outputs;

import de.fraunhofer.iem.icognicrypt.IdeSupport.projects.Json.OutputJson;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public abstract class OutputManager implements IHasOutputs
{
    protected OutputJson DebugJson;
    protected OutputJson ReleaseJson;


    @Override
    public abstract void InvalidateOutput();

    @Override
    public Iterable<String> GetOutputs(Set<OutputFinderOptions.Flags> options)
    {
        HashSet<String> result = new HashSet<>();

        if (OutputFinderOptions.contains(options, OutputFinderOptions.Flags.Debug)  && DebugJson != null)
        {
            result.add(GetDebugOutputPath());
        }

        if (OutputFinderOptions.contains(options, OutputFinderOptions.Flags.Release) && ReleaseJson != null)
        {
            result.add(GetReleaseOutputPath());
        }
        return result;
    }

    @Override
    public String GetDebugOutputPath()
    {
        return GetOutputFilePath(DebugJson, true);
    }

    @Override
    public String GetReleaseOutputPath()
    {
        return GetOutputFilePath(ReleaseJson, true);
    }

    private String GetOutputFilePath(OutputJson outputJson, boolean absolute)
    {
        if (outputJson == null)
            return null;
        return outputJson.GetOutputFilePath(absolute);
    }
}
