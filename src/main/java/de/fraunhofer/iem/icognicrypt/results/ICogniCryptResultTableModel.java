package de.fraunhofer.iem.icognicrypt.results;

import javax.swing.table.TableModel;

public interface ICogniCryptResultTableModel extends TableModel, IResultsProviderListener
{
    void AddError(CogniCryptError cogniCryptError);

    CogniCryptError GetResultAt(int selectedRow);

    void ClearErrors();
}
