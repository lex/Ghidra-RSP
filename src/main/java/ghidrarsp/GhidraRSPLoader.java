/* ###
* IP: GHIDRA
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ghidrarsp;

import java.io.IOException;
import java.util.*;
import java.nio.file.Files;
import java.io.File;

import docking.widgets.filechooser.GhidraFileChooser;
import ghidra.app.util.Option;
import ghidra.app.util.OptionUtils;
import ghidra.app.util.bin.ByteProvider;
import ghidra.app.util.opinion.AbstractProgramWrapperLoader;
import ghidra.app.util.opinion.LoadSpec;
import ghidra.framework.model.DomainObject;
import ghidra.program.flatapi.FlatProgramAPI;
import ghidra.program.model.lang.LanguageCompilerSpecPair;
import ghidra.program.model.listing.Program;
import ghidra.program.model.util.CodeUnitInsertionException;
import ghidra.util.exception.CancelledException;
import ghidra.util.SystemUtilities;
import ghidra.util.exception.DuplicateNameException;
import ghidra.app.util.opinion.Loader;

/**
* TODO: Provide class-level documentation that describes what this loader does.
*/
public class GhidraRSPLoader extends AbstractProgramWrapperLoader {

    private static String OPTION_IS_BOOT = "Is Boot";
    private static String OPTION_DMEM_FILE = "DMEM File";
    private static int DMEM_SIZE = 0x1000;

    @Override
    public String getName() {
        return "Nintendo 64 RSP loader";
    }

    @Override
    public Collection<LoadSpec> findSupportedLoadSpecs(ByteProvider provider) throws IOException {
        List<LoadSpec> loadSpecs = new ArrayList<>();

        loadSpecs.add(new LoadSpec(this, 0, new LanguageCompilerSpecPair("RSP:BE:32:default", "default"), true));

        return loadSpecs;
    }

    @Override
    protected void load(Program program, ImporterSettings settings)
    throws CancelledException, IOException {

        FlatProgramAPI api = new FlatProgramAPI(program);

        File dmemFile = getDmemFile(settings.options());
        int dmemEnd = 0;
        if (dmemFile != null) {
            // validateOptions() is not called for headless imports, so check again here
            String error = checkDmemFile(dmemFile);
            if (error != null)
                throw new IOException(error);

            byte[] bytes = Files.readAllBytes(dmemFile.toPath());
            dmemEnd = bytes.length;
            try {
                api.createMemoryBlock("dmem", api.toAddr(0), bytes,false).setPermissions(true, true, false);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CancelledException();
            }
        }

        boolean isBoot = OptionUtils.getBooleanOptionValue(OPTION_IS_BOOT, settings.options(), false);

        ByteProvider provider = settings.provider();
        byte[] imem = provider.readBytes(0, provider.length());
        try {
            int textBase = isBoot ? 0x1000 : 0x1080;
            api.createMemoryBlock("imem", api.toAddr(textBase), imem, false).setPermissions(true, false, true);
            api.addEntryPoint(api.toAddr(textBase));
            if (dmemEnd < DMEM_SIZE)
                program.getMemory().createUninitializedBlock("dmem.uninit", api.toAddr(dmemEnd), DMEM_SIZE - dmemEnd, false).setPermissions(true, true, false);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CancelledException();
        }

        try {
            api.createData(api.toAddr(0xFC0), new OSTask().toDataType());
        } catch (CodeUnitInsertionException | DuplicateNameException | IOException e) {
            e.printStackTrace();
            throw new CancelledException();
        }

    }

    /**
     * Returns the DMEM file from the "DMEM File" option, or asks for one with a file chooser
     * when the option is empty. In headless mode there is no chooser, so DMEM is left empty.
     */
    private File getDmemFile(List<Option> options) {
        String path = OptionUtils.getOption(OPTION_DMEM_FILE, options, "");
        if (!path.isEmpty())
            return new File(path);

        if (SystemUtilities.isInHeadlessMode())
            return null;

        while (true)
        {
            GhidraFileChooser chooser = new GhidraFileChooser(null);
            chooser.setTitle("Choose a ucode rodata file (the content of DMEM)");
            File f = chooser.getSelectedFile();
            if (f == null)
                return null;

            if (checkDmemFile(f) == null)
                return f;
        }
    }

    /** Returns an error message if the file can't be used as DMEM, or null if it can. */
    private static String checkDmemFile(File f) {
        if (!f.isFile())
            return "DMEM file not found: " + f;
        if (f.length() > DMEM_SIZE)
            return "DMEM file is larger than 0x1000 bytes: " + f;
        return null;
    }

    @Override
    public List<Option> getDefaultOptions(ByteProvider provider, LoadSpec loadSpec,
    DomainObject domainObject, boolean isLoadIntoProgram, boolean mirrorFsLayout) {
        List<Option> list =
        super.getDefaultOptions(provider, loadSpec, domainObject, isLoadIntoProgram, mirrorFsLayout);

        list.add(new Option(OPTION_IS_BOOT, false, Boolean.class, Loader.COMMAND_LINE_ARG_PREFIX + "-isBoot"));
        list.add(new Option(OPTION_DMEM_FILE, "", String.class, Loader.COMMAND_LINE_ARG_PREFIX + "-dmemFile"));

        return list;
    }

    @Override
    public String validateOptions(ByteProvider provider, LoadSpec loadSpec, List<Option> options, Program program) {
        String path = OptionUtils.getOption(OPTION_DMEM_FILE, options, "");
        if (!path.isEmpty()) {
            String error = checkDmemFile(new File(path));
            if (error != null)
                return error;
        }

        return super.validateOptions(provider, loadSpec, options, program);
    }
}
