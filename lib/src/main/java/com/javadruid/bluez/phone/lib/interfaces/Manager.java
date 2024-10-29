package com.javadruid.bluez.phone.lib.interfaces;

import org.freedesktop.dbus.DBusMap;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * @see https://github.com/rilmodem/ofono/blob/master/doc/manager-api.txt
 */
@DBusInterfaceName(Manager.DBUS_INTERFACE_NAME)
public interface Manager extends DBusInterface {

    static final String DBUS_INTERFACE_NAME = "org.ofono.Manager";

    DBusMap<ObjectPath, DBusMap<String, Variant<?>>> GetModems();

}
