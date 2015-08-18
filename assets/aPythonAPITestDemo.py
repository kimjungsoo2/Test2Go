## testAPI
## Jung Soo Kim
## May-3-2013
## This verifies aPython framework APIs on android device.
import android, time
from operator import itemgetter
import unittest

class aPythonAPITest(unittest.TestCase):

	def setUp(self):
		self.android = android.connect()
		self.screen = self.android.ui.screen()

	def test035_screenScrollTo(self):
		'''test035_screenScrollTo'''
		try:
			self.android.input.menu()
			self.android.ui.waitfor(anyof=[self.android.ui.widgetspec(text='System settings')]).tap()
			self.android.ui.scrollto(anyof=[self.android.ui.widgetspec(text='About phone')])
			self.android.input.back()
		except Exception, e:
			print "---- Fail"
			raise e

	def test036_screenUnlock(self):
		'''test036_screenUnlock'''
		try:
			while self.android.device.is_screen_on():
				self.android.input.power()
				time.sleep(0.5)
			self.android.ui.unlock()
			if self.android.device.is_screen_on():
				print "--", "Screen unlocked"
			else:
				print "---- Fail"
		except Exception, e:
			print "---- Fail"
			raise e

	def test039_pressVolumeUpKey(self):
		'''test039_pressVolumeUpKey'''
		try:
			for i in range(3):
				self.android.input.key_down('VOLUME_UP')
				self.android.input.key_up('VOLUME_UP')
		except Exception, e:
			print "---- Fail"
			raise e

	def test040_pressVolumeDownKey(self):
		'''test040_pressVolumeDownKey'''
		try:
			for i in range(3):
				self.android.input.key_down('VOLUME_DOWN')
				self.android.input.key_up('VOLUME_DOWN')
		except Exception, e:
			print "---- Fail"
			raise e

	def test042_touchDownandDrag(self):
		'''test042_touchDownandDrag'''
		try:
			l = []
			for w in self.screen.widgets():
				if any([w.type() == "com.motorola.homescreen.BubbleTextView", w.type() == "com.android.launcher2.BubbleTextView"]) and len(w.text()) > 0:
					l.append({'id':w.id(), 'text':w.text(), 'x':w.x(), 'y':w.y()})

			l = sorted(l, key=itemgetter('y', 'x'))

			cal1 = self.android.ui.waitfor(anyof=[self.android.ui.widgetspec(text=l[0]['text'])])
			posXCal1 = cal1.x() + cal1.width() / 2
			posYCal1 = cal1.y() + cal1.height() / 2
			self.android.input.touch_down(posXCal1, posYCal1)
			self.android.input.drag(posXCal1, posYCal1, posXCal1, posYCal1 - cal1.height())
			time.sleep(1)
			cal2 = self.android.ui.waitfor(anyof=[self.android.ui.widgetspec(text=l[0]['text'])])
			if cal1.y() <> cal2.y():
				print "--", "Target widget moved"
			else:
				print "--", "Failed"

			posXCal2 = cal2.x() + cal2.width() / 2
			posYCal2 = cal2.y() + cal2.height() / 2
			self.android.input.touch_down(posXCal2, posYCal2)
			self.android.input.drag(posXCal2, posYCal2, posXCal1, posYCal2 + cal2.height())
		except Exception, e:
			print "---- Fail"
			raise e

	def test044_inputTextTest(self):
		'''test044_inputTextTest'''
		try:
			self.android.ui.waitfor(anyof=[self.android.ui.widgetspec(text='Text Messaging'),
										   self.android.ui.widgetspec(text='Messaging')]).tap()
			self.android.ui.waitfor(anyof=[self.android.ui.widgetspec(id='action_compose_new')]).tap()
			time.sleep(3)
			self.android.ui.waitfor(anyof=[self.android.ui.widgetspec(id='embedded_text_editor')]).tap()
			time.sleep(1)
			self.android.input.text("This is a sample text.")
			time.sleep(1)
			editor = self.android.ui.waitfor(anyof=[self.android.ui.widgetspec(id='embedded_text_editor')])
			if editor.text() == "This is a sample text.":
				print "--", "Text is entered correctly", editor.text()
			else:
				print "--", "failed"
			time.sleep(1)
			self.android.input.back()
			self.android.input.back()
			self.android.ui.waitfor(anyof=[self.android.ui.widgetspec(text='OK')]).tap()
			self.android.input.back()
		except Exception, e:
			print "---- Fail"
			raise e

if __name__ == '__main__':
	unittest.main()
