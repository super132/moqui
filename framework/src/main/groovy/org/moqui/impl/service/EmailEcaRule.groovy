/*
 * This software is in the public domain under CC0 1.0 Universal.
 * 
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 * 
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package org.moqui.impl.service

import org.moqui.impl.actions.XmlAction
import org.moqui.impl.context.ExecutionContextFactoryImpl
import org.moqui.context.ExecutionContext

import javax.mail.internet.MimeMessage
import javax.mail.Address
import javax.mail.Multipart
import javax.mail.BodyPart
import javax.mail.Part
import javax.mail.Header

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EmailEcaRule {
    protected final static Logger logger = LoggerFactory.getLogger(EmailEcaRule.class)

    protected Node emecaNode
    protected String location

    protected XmlAction condition = null
    protected XmlAction actions = null

    EmailEcaRule(ExecutionContextFactoryImpl ecfi, Node emecaNode, String location) {
        this.emecaNode = emecaNode
        this.location = location

        // prep condition
        if (emecaNode.condition && emecaNode.condition[0].children()) {
            // the script is effectively the first child of the condition element
            condition = new XmlAction(ecfi, (Node) emecaNode.condition[0].children()[0], location + ".condition")
        }
        // prep actions
        if (emecaNode.actions) {
            actions = new XmlAction(ecfi, (Node) emecaNode.actions[0], location + ".actions")
        }
    }

    // Node getEmecaNode() { return emecaNode }

    void runIfMatches(MimeMessage message, ExecutionContext ec) {

        try {
            ec.context.push()

            Map<String, Object> fields = new HashMap()
            ec.context.put("fields", fields)

            List<String> toList = []
            for (Address addr in message.getRecipients(MimeMessage.RecipientType.TO)) toList.add(addr.toString())
            fields.put("toList", toList)

            List<String> ccList = []
            for (Address addr in message.getRecipients(MimeMessage.RecipientType.CC)) toList.add(addr.toString())
            fields.put("ccList", ccList)

            List<String> bccList = []
            for (Address addr in message.getRecipients(MimeMessage.RecipientType.BCC)) toList.add(addr.toString())
            fields.put("bccList", bccList)

            fields.put("from", message.getFrom()?.getAt(0)?.toString())
            fields.put("subject", message.getSubject())
            fields.put("sentDate", message.getSentDate())
            fields.put("receivedDate", message.getReceivedDate())
            fields.put("bodyPartList", makeBodyPartList(message))

            Map<String, Object> headers = new HashMap()
            ec.context.put("headers", headers)
            for (Header header in message.allHeaders) {
                if (headers.get(header.name)) {
                    Object hi = headers.get(header.name)
                    if (hi instanceof List) { hi.add(header.value) }
                    else { headers.put(header.name, [hi, header.value]) }
                } else {
                    headers.put(header.name, header.value)
                }
            }

            // run the condition and if passes run the actions
            boolean conditionPassed = true
            if (condition) conditionPassed = condition.checkCondition(ec)
            if (conditionPassed) {
                if (actions) actions.run(ec)
            }
        } finally {
            ec.context.pop()
        }
    }

    protected List<String> makeBodyPartList(Part part) {
        List<String> bodyPartList = []
        Object content = part.getContent()
        if (content instanceof CharSequence) {
            bodyPartList.add(content.toString())
        } else if (content instanceof Multipart) {
            int count = ((Multipart) content).getCount()
            for (int i = 0; i < count; i++) {
                BodyPart bp = ((Multipart) content).getBodyPart(i)
                bodyPartList.addAll(makeBodyPartList(bp))
            }
        }
        return bodyPartList
    }
}
