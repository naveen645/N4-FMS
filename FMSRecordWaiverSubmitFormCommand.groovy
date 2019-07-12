/*
* Copyright (c) 2019 WeServe LLC. All Rights Reserved.
*
*/
import com.navis.external.framework.ui.AbstractFormSubmissionCommand
import com.navis.external.framework.util.EFieldChanges
import com.navis.framework.metafields.MetafieldIdFactory
import com.navis.framework.metafields.entity.EntityId
import com.navis.framework.util.message.MessageCollector
import org.apache.log4j.Level
import org.apache.log4j.Logger


/*
*
* @Author <a href="mailto:anaveen@servimostech.com">Naveen A</a>, 12/JULY/2019
*
* Requirements : This groovy is used to override the default submitFormCommand for INV_FORM_RECORD_WAIVER form.
*
* @Inclusion Location	: Incorporated as a code extension of the type FORM_SUBMISSION_INTERCEPTOR.Copy --> Paste this code (FMSRecordWaiverSubmitFormCommand.groovy)
*
* @Set up in the database backed variform - INV_FORM_RECORD_WAIVER- adding action link to call this command and execute it.
*
*/
class FMSRecordWaiverSubmitFormCommand extends AbstractFormSubmissionCommand {

    @Override
    void submit(String inVariformId, EntityId inEntityId, List<Serializable> inGkeys, EFieldChanges inOutFieldChanges, EFieldChanges inNonDbFieldChanges, Map<String, Object> inParams) {
        LOGGER.setLevel(Level.DEBUG)
        LOGGER.debug("FMSRecordWaiverSubmitFormCommand started execution!!!!!!!submit method")

        Map paramMap = new HashMap();
        Map results = new HashMap();
        paramMap.put("FIELD_CHANGES", inOutFieldChanges)
        paramMap.put("GKEYS", inGkeys)
        MessageCollector messageCollector = executeInTransaction("FMSRecordWaiverTransactionCallback", paramMap, results)
        if (messageCollector.hasError()) {
            registerMessageCollector(messageCollector)
        }

    }

    @Override
    void doBeforeSubmit(String inVariformId, EntityId inEntityId, List<Serializable> inGkeys, EFieldChanges inOutFieldChanges, EFieldChanges inNonDbFieldChanges, Map<String, Object> inParams) {
        LOGGER.setLevel(Level.DEBUG)
        LOGGER.debug("FMSRecordWaiverSubmitFormCommand started execution!!!!!!!!!!!!doBeforeSubmit method")

        if(inNonDbFieldChanges.hasFieldChange(MetafieldIdFactory.valueOf("customFlexFields.gnteCustomDFFNotes"))){
            String customNotes = (String) inNonDbFieldChanges.findFieldChange(MetafieldIdFactory.valueOf("customFlexFields.gnteCustomDFFNotes")).getNewValue()
            inOutFieldChanges.setFieldChange(MetafieldIdFactory.valueOf("gnteNotes"),customNotes);
        }

    }
    private static final Logger LOGGER = Logger.getLogger(FMSRecordWaiverSubmitFormCommand.class);
}
