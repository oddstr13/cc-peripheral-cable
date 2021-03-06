package xfel.mods.cccable.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.minecraft.item.ItemDye;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;

/**
 * An attachment of a specific peripheral to a specific computer on a specific
 * address.
 * 
 * Also used as security wrapper for the {@link IComputerAccess} passed from the
 * computer
 * 
 * @author Xfel
 * 
 */
public class PeripheralAttachment implements IComputerAccess {
	public static final String[] colorNames = { "green", "brown", "black",
			"pink", "yellow", "orange", "magenta", "purple", "cyan", "red",
			"white", "lightBlue", "lightGray", "gray", "lime", "blue" };

	private IPeripheral peripheral;

	private int colorTag;

	private IComputerAccess computer;

	private HashSet<String> myMounts;

	private boolean attached;

	private HashMap<String, Integer> methodMap;
	private String[] methods;
	private String type;

	private String virtualSide;

	PeripheralAttachment(IPeripheral peripheral, int colorTag,
			IComputerAccess computer) {
		super();
		this.peripheral = peripheral;
		this.colorTag = colorTag;
		this.computer = computer;
		this.virtualSide = getVirtualSide(computer.getAttachmentSide(),
				colorTag);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + colorTag;
		result = prime * result
				+ ((computer == null) ? 0 : System.identityHashCode(computer));
		result = prime * result
				+ ((peripheral == null) ? 0 : peripheral.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PeripheralAttachment other = (PeripheralAttachment) obj;
		if (colorTag != other.colorTag) {
			if (computer != other.computer) {
				return false;
			}
		}
		if (peripheral == null) {
			if (other.peripheral != null) {
				return false;
			}
		} else if (!peripheral.equals(other.peripheral)) {
			return false;
		}
		return true;
	}

	// does the attach op
	void attach() {
		// System.out.println("attach "+this);
		type = peripheral.getType();
		methods = peripheral.getMethodNames();
		methodMap = new HashMap<String, Integer>();

		for (int i = 0; i < methods.length; i++) {
			methodMap.put(methods[i], Integer.valueOf(i));
		}

		myMounts = new HashSet<String>();
		attached = true;

		peripheral.attach(computer);
		computer.queueEvent("peripheral", new Object[] { virtualSide });
	}

	// does the detach op
	void detach() {
		// System.out.println("detach "+this);
		peripheral.detach(computer);

		for (String loc : myMounts) {
			computer.unmount(loc);
		}

		myMounts = null;
		attached = false;

		computer.queueEvent("peripheral_detach", new Object[] { virtualSide });
	}

	@Override
	public int createNewSaveDir(String subPath) {
		if (!attached) {
			throw new RuntimeException("You are not attached to this Computer");
		}
		return computer.createNewSaveDir(subPath);
	}

	@Override
	public String mountSaveDir(String desiredLocation, String subPath, int id,
			boolean readOnly, long spaceLimit) {
		if (!attached) {
			throw new RuntimeException("You are not attached to this Computer");
		}
		String dir = computer.mountSaveDir(desiredLocation, subPath, id,
				readOnly, spaceLimit);
		myMounts.add(dir);
		return dir;
	}

	@Override
	public String mountFixedDir(String desiredLocation, String path,
			boolean readOnly, long spaceLimit) {
		if (!attached) {
			throw new RuntimeException("You are not attached to this Computer");
		}
		String dir = computer.mountFixedDir(desiredLocation, path, readOnly,
				spaceLimit);
		myMounts.add(dir);
		return dir;
	}

	@Override
	public void unmount(String location) {
		if (!attached) {
			throw new RuntimeException("You are not attached to this Computer");
		}
		if (!myMounts.remove(location)) {
			throw new RuntimeException("You didn't mount this location");
		}
		computer.unmount(location);
	}

	@Override
	public int getID() {
		if (!attached) {
			throw new RuntimeException("You are not attached to this Computer");
		}
		return computer.getID();
	}

	@Override
	public void queueEvent(String event) {
		if (!attached) {
			throw new RuntimeException("You are not attached to this Computer");
		}
		computer.queueEvent(event);
	}

	@Override
	public void queueEvent(String event, Object[] arguments) {
		if (!attached) {
			throw new RuntimeException("You are not attached to this Computer");
		}
		computer.queueEvent(event, arguments);
	}

	/**
	 * Call a peripehral method...
	 * 
	 * @param methodName
	 *            the method name
	 * @param args
	 *            the arguments
	 * @return the call result
	 * @throws Exception
	 *             if some error occurs
	 * @see IPeripheral#callMethod(IComputerAccess, int, Object[])
	 */
	public Object[] call(String methodName, Object[] args) throws Exception {
		assert (this.attached == true);
		if (this.methodMap.containsKey(methodName)) {
			int method = this.methodMap.get(methodName).intValue();

			return this.peripheral.callMethod(this, method, args);
		}
		throw new Exception("No such method " + methodName);
	}

	/**
	 * Retrieve the peripheral method table.
	 * 
	 * @return the method list (a map to be used as lua table)
	 */
	public Map<Integer, String> getMethods() {
		Map<Integer, String> table = new HashMap<Integer, String>();
		for (int i = 0; i < methods.length; i++) {
			table.put(Integer.valueOf(i + 1), methods[i]);
		}
		return table;
	}

	/**
	 * Returns the peripheral type.
	 * 
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Formats computer side and color tag into the virtual side name used in
	 * code.
	 * 
	 * @param side
	 *            the computer side the cable is attached on
	 * @param colorTag
	 *            the color tag the peripheral is attached on.
	 * @return a string in the format [side]:[color name]
	 */
	public static String getVirtualSide(String side, int colorTag) {
		StringBuilder sb = new StringBuilder(side);
		sb.append(':');
		sb.append(ItemDye.dyeColorNames[colorTag]);
		return sb.toString();
	}

	private static HashMap<PeripheralAttachment, PeripheralAttachment> attachments = new HashMap<PeripheralAttachment, PeripheralAttachment>();

	/**
	 * Creates a new <code>PeripheralAttachment</code> object and registers it.
	 * 
	 * @param peripheral
	 *            the peripheral
	 * @param colorTag
	 *            the color tag
	 * @param computer
	 *            the computer to attach to
	 */
	public static synchronized void attachPeripheral(IPeripheral peripheral,
			int colorTag, IComputerAccess computer) {
		PeripheralAttachment att = new PeripheralAttachment(peripheral,
				colorTag, computer);

		if (!attachments.containsKey(att)) {
			attachments.put(att, att);

			att.attach();
		}
	}

	/**
	 * Detaches and removes a <code>PeripheralAttachment</code> object
	 * 
	 * @param peripheral
	 *            the peripheral
	 * @param colorTag
	 *            the color tag
	 * @param computer
	 *            the computer
	 */
	public static synchronized void detachPeripheral(IPeripheral peripheral,
			int colorTag, IComputerAccess computer) {
		PeripheralAttachment att = new PeripheralAttachment(peripheral,
				colorTag, computer);

		if (attachments.containsKey(att)) {
			attachments.remove(att).detach();
		}
	}

	/**
	 * Looks up and retrieves a <code>PeripehralAttachment</code> object from
	 * the table.
	 * 
	 * @param peripheral
	 *            the peripheral
	 * @param colorTag
	 *            the color tag
	 * @param computer
	 *            the computer
	 * @return the <code>PeripehralAttachment</code> object
	 */
	public static synchronized PeripheralAttachment getComputerWrapper(
			IPeripheral peripheral, int colorTag, IComputerAccess computer) {
		PeripheralAttachment att = new PeripheralAttachment(peripheral,
				colorTag, computer);

		return attachments.get(att);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PeripheralAttachment [peripheral=");
		builder.append(peripheral.getClass().getName());
		builder.append(", colorTag=");
		builder.append(colorTag);
		builder.append(", computer=");
		builder.append(computer.getID());
		builder.append(", cside=");
		builder.append(computer.getAttachmentSide());
		builder.append(", methods=");
		builder.append(Arrays.toString(methods));
		builder.append("]");
		return builder.toString();
	}

	@Override
	public String getAttachmentSide() {
		return virtualSide;
	}
}
